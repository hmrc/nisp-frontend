
$(document).ready(function(){

	var $breadCrumb = $("#global-breadcrumb nav ol li a");
	$breadCrumb.click(function(){
		event.preventDefault();
		$href = $(this).attr('href')
		$path = $href.substring($href.lastIndexOf("/")+1);
	});

if($("form").length) {


    if($("#whatWillYouDoNext-8:checked").length === 0) {
        $(".other-follow").css("display","none");
    }


	if($(".js-error-summary-messages").length) {
	    var $inputResearch = $("#research-0:checked").val();
	    var $followUp = $("#whatWillYouDoNext-8:checked").length;
	    if($followUp === 0) {					
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

	if($("input[name='whatWillYouDoNext']").length) {
		var $followUpOther = ($("input[name=whatWillYouDoNext]"));
		$followUpOther.change(function() {
			if($('input[name=whatWillYouDoNext]:checked').val() === "8")
				$(".other-follow").css("display","inline-block");
			else
				$(".other-follow").css('display','none');
		});
	}
}

});
