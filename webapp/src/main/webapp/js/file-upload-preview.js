(function() {
	function hidePreview(preview, image) {
		if (!preview || !image) {
			return;
		}
		if (image.dataset.previewObjectUrl) {
			URL.revokeObjectURL(image.dataset.previewObjectUrl);
			delete image.dataset.previewObjectUrl;
		}
		image.removeAttribute("src");
		preview.hidden = true;
		var container = preview.closest("[data-image-preview-container]");
		if (container) {
			container.classList.remove("has-image-preview");
		}
	}

	function showPreview(input, preview, image) {
		var file = input.files && input.files.length ? input.files[0] : null;
		if (!file || !file.type || !file.type.startsWith("image/")) {
			hidePreview(preview, image);
			return;
		}

		if (image.dataset.previewObjectUrl) {
			URL.revokeObjectURL(image.dataset.previewObjectUrl);
		}

		var objectUrl = URL.createObjectURL(file);
		image.dataset.previewObjectUrl = objectUrl;
		image.src = objectUrl;
		preview.hidden = false;
		var container = preview.closest("[data-image-preview-container]");
		if (container) {
			container.classList.add("has-image-preview");
		}
	}

	function bindInput(input) {
		var container = input.closest("[data-image-preview-container]");
		var preview = container ? container.querySelector("[data-image-preview]") : null;
		var image = preview ? preview.querySelector("img") : null;
		if (!container || !preview || !image) {
			return;
		}

		input.addEventListener("change", function() {
			showPreview(input, preview, image);
		});

		var form = input.form;
		if (form) {
			form.addEventListener("reset", function() {
				window.setTimeout(function() {
					hidePreview(preview, image);
				}, 0);
			});
		}
	}

	document.addEventListener("DOMContentLoaded", function() {
		document.querySelectorAll("[data-image-preview-input]").forEach(bindInput);
	});
})();
