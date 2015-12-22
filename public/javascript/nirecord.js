$(document).ready(function(){
  var allPanels = $('.accordion > dd').hide();
  var allHeaders = $('.accordion > dt');

  $('.accordion > dt.expandable').click(function() {
    var taxYear = $(this).find(".ni-years > abbr:first-child").text()

    if(!$(this).hasClass('active')) {
      $(this)
        .addClass('active')
        .next()
        .show();

      $(this).find("a").html("Hide <span class=\"visuallyhidden\"><abbr title=\""+taxYear+"\">" + taxYear + "</span> details");

      $(this).attr({'aria-expanded':'true'});
    } else {
      $(this)
        .removeClass('active')
        .next()
        .hide();

      $(this).find("a").html("View <span class=\"visuallyhidden\"><abbr title=\""+taxYear+"\">" + taxYear + "</abbr></span> details");

      $(this).attr({'aria-expanded':'false'});
    }

    return false;
  });
});
