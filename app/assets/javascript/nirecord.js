    $(document).ready(function(){

        var summary = $(".summary-list-item");

        $(summary).on("click enter", function(){
            var nirLink = document.getElementById("nir-external-link");
            var citizenLink = document.getElementById("citizenAdviceLink");
            var moneyAdviceLink = document.getElementById("moneyAdviceLink");
            var pensionLink = document.getElementById("pensionWiseLink");
            var professionalAdvice = document.getElementById("details-content-0").attributes[2].value
            var nirSection = document.getElementById("details-content-3").attributes[2].value
            if (professionalAdvice === "true" || nirSection === "true") {
                nirLink.tabIndex = "1";
                citizenLink.tabIndex = "1";
                moneyAdviceLink.tabIndex = "1 ";
                pensionLink.tabIndex = "1";
            } else {
                nirLink.tabIndex = "-1";
                citizenLink.tabIndex = "-1";
                moneyAdviceLink.tabIndex = "-1";
                pensionLink.tabIndex = "-1";
            }
        });

        var allPanels = $('.accordion > dd').hide();
        var allHeaders = $('.accordion > dt');
        var currentLanguage = $('#current-language').text()

      $(".hide-details").hide();


      $('.accordion > dt.expandable').click(function() {
         var taxYear = $(this).find(".ni-years").text()
         var showDetailsEnglish = 'View <span class="visuallyhidden">'+taxYear+'</span>details'
         var showDetailsWelsh = 'Gweld y manylion<span class="visuallyhidden">'+taxYear+'</span>'

         var hideDetailsEnglish ='Hide <span class="visuallyhidden">'+taxYear+'</span>details'
         var hideDetailsWelsh = 'Cuddio manylion<span class="visuallyhidden">'+taxYear+'</span>'

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
