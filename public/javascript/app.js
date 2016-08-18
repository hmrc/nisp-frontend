
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

if($("form").length) {

  if($("#research-0:checked").length === 0) {
      $(".email").css("display","none");
		}

  if($("#followupcall-10:checked").length === 0) {
      $(".other-follow").css("display","none");
		}

	if($(".js-error-summary-messages").length) {
	    var $inputResearch = $("#research-0:checked").val();
	    var $followUp = $("#followupcall-10:checked").length;
	    if($followUp === 0) {
					console.log($followUp);
      	$(".other-follow").css("display","none");
			}

			if(!$inputResearch) {
					$(".email").css("display","none");
			}

	}

	if($("input[name='research']").length) {
		var $inputResearch = $("input[name='research']");
		var $email = $("input[name='email']");
		$inputResearch.change(function() {
				console.log("I am in!!!!");
			if($(this).val() === '0') {
					$(".email").css("display","inline-block");
			}
			else {
	            $(".email").css('display','none');
	            if($email.val().length) {
	                $email.val('');
	            }
			}
		});
	}

	if($("input[name='followupcall']").length) {
    		var $followUpCall = ($("#followupcall-10"));
    		var $otherFollowUp = $("input[name='otherfollowup']");
    	//	if($(".form-field--error").length)
    		//    $(".other-follow").css("display","inline-block")
    		$followUpCall.click(function() {
    			if($(this).prop('checked'))
    					$(".other-follow").css("display","inline-block");
    			else
    					$(".other-follow").css('display','none')
    		});
    	}

}

});
