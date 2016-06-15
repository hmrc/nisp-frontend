
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

	if($("input[name='research']").length) {
		var $inputResearch = $("input[name='research']");
		var $email = $("input[name='email']");
		var $errorNotification = $(".error-notification");
		if($(".form-field--error").length)
			$(".email").css("display","inline-block")
		$inputResearch.change(function() {
			if($(this).val() === '0') 
					$(".email").css("display","inline-block");
			else {					
					$(".email").css('display','none');
					if($errorNotification.length) {
						$errorNotification.remove();
						$(".error-summary").remove(); 
						$("label").removeClass("form-field--error");
					}

					if($email.val().length) {
          	$email.val('');
					}
			}
		});
	}
});
