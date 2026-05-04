#!/bin/sh
set -eu

# Ask for pampero username
while true; do
	printf "Enter your pampero username: "
	read -r input_user
	if [ -n "$input_user" ]; then
		PAMPERO_USER="$input_user"
		break
	fi
	echo "Username cannot be empty. Please try again."
done
HOST="pampero.itba.edu.ar"

YEAR=$(date +"%Y")
MONTH=$(date +"%m")
DAY=$(date +"%d")

PAMPERO_APP_DIR=/home/$PAMPERO_USER/PAW
LOCAL_APP_WAR=./webapp/target/webapp.war
PAMPERO_APP_WAR=$PAMPERO_APP_DIR/webapp.war
PAMPERO_BACKUP_FILE=$PAMPERO_APP_DIR/PAW-DB-$YEAR-$MONTH-$DAY.sql

PAMPERO_HOST=10.16.1.110
PAMPERO_GROUP_USER=paw-2026a-14

PROD_PASS=$(cat ./config/pampero.properties | grep "db.password" | cut -d'=' -f2)

# Check if Maven is installed
command -v mvn >/dev/null 2>&1 || {
	echo "[deploy] Maven is required but not installed."
	exit 1
}

# Only allow deploy from main branch
CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [ "$CURRENT_BRANCH" != "main" ]; then
	echo "[deploy] Deploy is only allowed from the main branch."
	echo "[deploy] Please switch to the main branch and try again."
	exit 1
fi

# Check if local main is up to date with origin/main
git fetch origin main
LOCAL_COMMIT="$(git rev-parse HEAD)"
REMOTE_COMMIT="$(git rev-parse origin/main)"

if [ "$LOCAL_COMMIT" != "$REMOTE_COMMIT" ]; then
	echo "[deploy] Your local main branch is not up to date with origin/main."
	echo "[deploy] Please pull the latest changes and try again."
	exit 1
fi

# Check if local main is up to date with bitbucket remote
git fetch bitbucket main
BITBUCKET_COMMIT="$(git rev-parse bitbucket/main)"
if [ "$LOCAL_COMMIT" != "$BITBUCKET_COMMIT" ]; then
	echo "[deploy] Your local main branch is not up to date with bitbucket/main."
	echo "[deploy] Please push your changes to bitbucket and try again."
	exit 1
fi

# Compile the project
echo "[deploy] Compiling the project..."
mvn clean -Ppampero package

# Ensure the remote app directory exists before copying the artifact
ssh -o "StrictHostKeyChecking=no" "$PAMPERO_USER@$HOST" "mkdir -p '$PAMPERO_APP_DIR'"

# Copy the war to the remote server
scp "$LOCAL_APP_WAR" "$PAMPERO_USER@$HOST:$PAMPERO_APP_DIR/"

# Backup db and deploy
ssh -tt -o "StrictHostKeyChecking=no" "$PAMPERO_USER@$HOST" "touch '$PAMPERO_BACKUP_FILE' && PGPASSWORD='$PROD_PASS' pg_dump -h '$PAMPERO_HOST' -U '$PAMPERO_GROUP_USER' -f '$PAMPERO_BACKUP_FILE' && printf 'put %s %s\n' '$PAMPERO_APP_WAR' 'web/app.war' | sftp '$PAMPERO_GROUP_USER@$PAMPERO_HOST'"

echo "[deploy] Backup of the database created at $PAMPERO_BACKUP_FILE on the remote server."
echo "[deploy] Deployment completed successfully."
echo "[deploy] Check http://pawserver.it.itba.edu.ar/paw-2026a-14/."
