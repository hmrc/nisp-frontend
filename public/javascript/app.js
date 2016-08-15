
$(document).ready(function(){
	
	var $breadCrumb = $("#global-breadcrumb nav ol li a");
	$breadCrumb.click(function(){
		event.preventDefault();
		$href = $(this).attr('href')		
		$path = $href.substring($href.lastIndexOf("/")+1);		
		ga('send', {
  			hitType: 'event',
  			eventCategory: 'other-global',
  			eventAction: 'click',
  			eventLabel: "'"+$path+"'",
				hitCallback: function() {
      			window.location.href = $href;
				}
		});		
		 	
	});

	if($("#error-summary-display").length) {
	    var $inputResearch = $("input[name='research']:checked").val();
	    var $followUp = $("input[name='followupcall']:checked").val();
	    if(!$inputResearch || $inputResearch === "1") {
	        $(".email").css("display","none");
	    }
	    if(!$followUp || $followUp < "11") {
            $(".other-follow").css("display","none");
        }
	}

	if($("input[name='research']").length) {
		var $inputResearch = $("input[name='research']");
		var $email = $("input[name='email']");
		$inputResearch.change(function() {
			if($(this).val() === '0') 
					$(".email").css("display","inline-block");
			else {					
                $(".email").css('display','none');
                if($email.val().length) {
                    $email.val('');
                }
			}
		});
	}

	if($("input[name='followupcall']").length) {
    		var $followUpCall = $("input[name='followupcall']");
    		var $otherFollowUp = $("input[name='otherfollowup']");
    		$(".other-follow").css("display","none")
    		if($(".form-field--error").length) {
    		    $(".other-follow").css("display","inline-block");
    		}
    		$followUpCall.change(function() {
    			if($(this).val() === '10')
    					$(".other-follow").css("display","inline-block");
    			else {
    					$(".other-follow").css('display','none');
    					if($otherFollowUp.val().length) {
              	            $otherFollowUp.val('');
    					}
    			}
    		});
    	}
});
