$(document).ready(function() {

    $(".full-width-banner__close").on("click", function (e) {
        e.preventDefault();
        GOVUK.setCookie("cysp-nisp-urBannerHide", "suppress_for_all_services", 99999999999);
        $("#full-width-banner").addClass("banner-panel-close");
    });

});