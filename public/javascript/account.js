$(document).ready(function(){

      var  $myTabs = $(".tablist a");

          //*****************************
          //  Embed ARIA tags
          //*****************************

      $(".tablist").addClass("js-tabs nav-tabs");
      $(".tablist ul:first-child")
                  .addClass("tabs-nav")
                  .attr('role','tablist');

      $(".tablist ul > li:first-child").addClass("active");
      $(".tab-panel")
                  .addClass("tab-pane")
                  .attr({"role":"tabpanel"});

      $('[role="tabpanel"]:last').attr({ 'aria-hidden' : 'true'});

      $("[role='tablist'] li").attr("role","presentation");

      $('[role="tablist"] li:first-child a').attr({'aria-selected' : 'true', 'tabindex' : '0'});   
     
     
          //*****************************
          // Reset tabs
          //*****************************     
      
      $myTabs.each(function(index){
           $tabElm = $($myTabs[index]).attr("id").substr(4);           
            if (index===0) {
              $("#"+$tabElm).show(); 
              $("#"+$tabElm).attr({'aria-hidden':'false'});
               window.scrollTo(0,0); 
            }
            else {
              $("#"+$tabElm).hide();
              $("#"+$tabElm).attr({'aria-hidden':'true'});              
            }         
      });
     
      $($myTabs).click(function(){
        $tabContent=$(this).attr('id').substr(4);
         $myTabs.each(function(index, value){
            $aTab = $($myTabs[index]).attr("id").substr(4);
            if(($aTab) === $tabContent) {
               $("#"+$aTab).show();
               $(".tabs-nav li").removeClass("active");
               $(this).parent().addClass("active");
               $("#"+$aTab).attr({'aria-hidden':'false'});
               $(this).attr({'aria-selected':'true'});
            }
            else {
               $("#"+$aTab).hide();
               $("#"+$aTab).attr({'aria-hidden':'true'});
               $(this).attr({'aria-selected':'false'});
            }
         });
         
        
         if($tabContent === "forecast-sp-value") {
            ga('send', {
                'page': '/checkmystatepension/forecast',
                'hitType': 'pageview',
                'title': 'forecast'
            });
            if(history.pushState) {
                history.pushState(null, null, '#forecast');
            }
            else {
                window.location.hash = '#forecast';
            }
              
        }
        else if($tabContent === "how-to-increase-it") {
            ga('send', {
                'page': '/checkmystatepension/how-to-increase-it',
                'hitType': 'pageview',
                'title': 'how-to-increase-it'
            });
            if(history.pushState) {
                history.pushState(null, null, '#how-to-increase-it');
            }
            else {
                window.location.hash = '#how-to-increase-it';
            }
             
        } 
        else if($tabContent === "cope") {
            ga('send', {
                  'page': '/checkmystatepension/cope',
                  'hitType': 'pageview',
                  'title': 'cope'
            });
            if(history.pushState) {
              history.pushState(null, null, '#cope');
            }
            else {
              window.location.hash = '#cope';
            }
        }         
        return false;
      })


		if(window.location.hash==="#forecast") {
        manageLocationHash('forecast');
 
     }
     else if(window.location.hash==="#cope") {
         manageLocationHash('cope');
     }
        
     else if(window.location.hash==="#how-to-increase-it") {
         manageLocationHash('how-to-increase-it');
     }

 function manageLocationHash(elm){
      
    $myTabs.each(function(index, value){
            $aTab = $($myTabs[index]).attr("id").substr(4);
            if($aTab.indexOf(elm)>=0) {  
               $("#"+$aTab).show();
               $(".tabs-nav li").removeClass("active");
               $(this).parent().addClass("active");
               $("#"+$aTab).attr({'aria-hidden':'false'});
               $(this).attr({'aria-selected':'true'});
                window.scrollTo(0,0); 
            }
            else {
               $("#"+$aTab).hide();
               $("#"+$aTab).attr({'aria-hidden':'true'});
               $(this).attr({'aria-selected':'false'});
            }
                  
     });
 

 }


 });
