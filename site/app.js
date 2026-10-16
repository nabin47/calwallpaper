// Points the download button at the APK of the latest GitHub Release.
// Without JavaScript, or if the API call fails, the button links to the Releases page.
(function () {
  "use strict";
  var REPO = "nabin47/calwallpaper";
  var button = document.getElementById("download");
  var meta = document.getElementById("download-meta");
  if (!button || !window.fetch) return;

  fetch("https://api.github.com/repos/" + REPO + "/releases/latest", {
    headers: { Accept: "application/vnd.github+json" }
  })
    .then(function (response) {
      if (!response.ok) throw new Error("HTTP " + response.status);
      return response.json();
    })
    .then(function (release) {
      var apk = (release.assets || []).find(function (asset) {
        return /\.apk$/i.test(asset.name);
      });
      if (!apk) return;
      var sizeMb = (apk.size / (1024 * 1024)).toFixed(1);
      button.href = apk.browser_download_url;
      button.textContent = "Download " + release.tag_name + " for Android";
      meta.textContent = "APK, " + sizeMb + " MB. Android 8.0 or newer. Checksums are on the release page.";
    })
    .catch(function () {
      // Keep the fallback link to the Releases page.
    });
})();
