$(document).ready(function(){
  var allPanels = $('.accordion > dd').hide();
  var allHeaders = $('.accordion > dt');
  $(".hide-details").hide();

  $('.accordion > dt.expandable').click(function() {
    var taxYear = $(this).find(".ni-years").text()

    if(!$(this).hasClass('active')) {
      $(this)
        .addClass('active')
        .next()
        .show();
      $(this).find("a.view-details").hide();
       $(this).find("a.hide-details").show();

      $(this).attr({'aria-expanded':'true'});
    } else {
      $(this)
        .removeClass('active')
        .next()
        .hide();
            $(this).find("a.view-details").show();
             $(this).find("a.hide-details").hide();

      $(this).attr({'aria-expanded':'false'});
    }

    return false;
  });
});
