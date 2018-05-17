jQuery(document).ready(function($){
  var isLateralNavAnimating = false;

  $('.menu-burger').on('click', function(e) {
    e.preventDefault();

    if( !isLateralNavAnimating ) {
      if($(this).parents('.csstransitions').length > 0 ) isLateralNavAnimating = true;

      $('body').toggleClass('navigation-is-open');
      $('.navigation-wrapper').one('webkitTransitionEnd otransitionend oTransitionEnd msTransitionEnd transitionend', function() {
        isLateralNavAnimating = false;
      });
    }
  });
});