/* global $ */
/* eslint-disable no-unused-vars, sonarjs/cognitive-complexity */
/* ^ Disabling a few checks for now rather than rewriting, while upgrading */

$(document).ready(function () {
  var $href;
  var $path;
  var $inputResearch;
  var $followUp;
  var $followUpOther;
  var $email;

  var $breadCrumb = $('#global-breadcrumb nav ol li a');
  $breadCrumb.click(function () {
    event.preventDefault();
    $href = $(this).attr('href');
    $path = $href.substring($href.lastIndexOf('/') + 1);
  });

  if ($('form').length) {
    var $otherFollow = $('.other-follow');

    if ($('#whatWillYouDoNext-8:checked').length === 0) {
      $otherFollow.css('display', 'none');
    }

    if ($('.js-error-summary-messages').length) {
      $inputResearch = $('#research-0:checked').val();
      $followUp = $('#whatWillYouDoNext-8:checked').length;
      if ($followUp === 0) {
        $otherFollow.css('display', 'none');
      }

      if (!$inputResearch) {
        $('.email').css('display', 'none');
      }
    }

    if ($("input[name='research']").length) {
      $inputResearch = $("input[name='research']");
      $email = $("input[name='email']");
      $inputResearch.change(function () {
        if ($(this).val() === '0') {
          $('.email').css('display', 'inline-block');
        } else {
          $('.email').css('display', 'none');
          if ($email.val().length) {
            $email.val('');
          }
        }
      });
    }

    if ($("input[name='whatWillYouDoNext']").length) {
      $followUpOther = $('input[name=whatWillYouDoNext]');
      $followUpOther.change(function () {
        if ($('input[name=whatWillYouDoNext]:checked').val() === '8')
          $otherFollow.css('display', 'inline-block');
        else $otherFollow.css('display', 'none');
      });
    }
  }
});

const printlink = document.getElementById('printLink');

if(printlink != null && printlink != 'undefined' ) {

  printlink.addEventListener("click", function (e) {
    e.preventDefault();
    window.print();
  });
};
