
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
	
});
