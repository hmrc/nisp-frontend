    $(document).ready(function(){
      var allPanels = $('.accordion > dd').hide();
      var allHeaders = $('.accordion > dt');
      var currentLanguage = $('#current-language').text()
      $(".hide-details").hide();

      $('.accordion > dt.expandable').click(function() {
         var taxYear = $(this).find(".ni-years").text()
         var showDetailsEnglish = 'View <span class="visuallyhidden">'+taxYear+'</span>details'
         var showDetailsWelsh = 'Edrychwch ar <span class="visuallyhidden">'+taxYear+'</span>fanylion'

         var hideDetailsEnglish ='Hide <span class="visuallyhidden">'+taxYear+'</span>details'
         var hideDetailsWelsh = 'Cuddio <span class="visuallyhidden">'+taxYear+'</span>manylion'

        if(!$(this).hasClass('active')) {
          $(this)
            .addClass('active')
            .next()
            .show();

           if(currentLanguage === "en")
               $(this).find("a.view-details").html(hideDetailsEnglish)
           else
               $(this).find("a.view-details").html(hideDetailsWelsh)

           $(this).find("a.view-details").attr({'aria-expanded':'true'});
           $(this).attr({'aria-expanded':'true'});

        } else {
          $(this)
            .removeClass('active')
            .next()
            .hide();

          if(currentLanguage === "en")
            $(this).find("a.view-details").html(showDetailsEnglish)
          else
            $(this).find("a.view-details").html(showDetailsWelsh)

          $(this).find("a.view-details").attr({'aria-expanded':'false'});
          $(this).attr({'aria-expanded':'false'});

        }

        return false;
      });
    });
