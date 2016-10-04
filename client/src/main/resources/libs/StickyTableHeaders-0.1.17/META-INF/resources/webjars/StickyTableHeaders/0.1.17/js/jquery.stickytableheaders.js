/*! Copyright (c) 2011 by Jonas Mosbech - https://github.com/jmosbech/StickyTableHeaders
	MIT license info: https://github.com/jmosbech/StickyTableHeaders/blob/master/license.txt */
// Modification by v6ak are discussed there: https://github.com/jmosbech/StickyTableHeaders/issues/89

;(function ($, window, undefined) {
	'use strict';

	var name = 'stickyTableHeaders',
		id = 0,
		defaults = {
			fixedOffset: 0,
			leftOffset: 0,
			marginTop: 0,
			objDocument: document,
			objHead: 'head',
			objWindow: window,
			scrollableArea: window
		};

	function Plugin (el, options) {
		// To avoid scope issues, use 'base' instead of 'this'
		// to reference this class from internal events and functions.
		var base = this;

		// Access to jQuery and DOM versions of element
		base.$el = $(el);
		base.el = el;
		base.id = id++;

		// Listen for destroyed, call teardown
		base.$el.bind('destroyed',
			$.proxy(base.teardown, base));

		// Cache DOM refs for performance reasons
		base.$clonedHeader = null;
		base.$originalHeader = null;

		// Keep track of state
		base.isSticky = false;
		base.hasBeenSticky = false;
		base.leftOffset = null;
		base.topOffset = null;

		var isGecko = navigator.userAgent.indexOf("Gecko/") != -1;
		var isTrident = navigator.userAgent.indexOf("Trident/") != -1;
		var async = isGecko || isTrident; // Those two engines have hiccups with sync approach when scrolling above the table top.
		if(window.console){
			console.log("isGecko: ", isGecko);
			console.log("isTrident: ", isTrident);
			console.log("async: ", async);
		}

		var queue = async ? function(f){setTimeout(f, 0);} : function(f){f();};

		base.init = function () {
			base.setOptions(options);

			base.$el.each(function () {
				var $this = $(this);

				// remove padding on <table> to fix issue #7
				$this.css('padding', 0);

				base.$originalHeader = $('thead:first', this);
				base.$clonedHeader = base.$originalHeader.clone();
				$this.trigger('clonedHeader.' + name, [base.$clonedHeader]);

				base.$clonedHeader.addClass('tableFloatingHeader');
				base.hideClonedHeader()

				base.$originalHeader.addClass('tableFloatingHeaderOriginal');

				base.$originalHeader.after(base.$clonedHeader);

				base.$printStyle = $('<style type="text/css" media="print">' +
					'.tableFloatingHeader{display:none !important;}' +
					'.tableFloatingHeaderOriginal{position:static !important;}' +
					'</style>');
				base.$head.append(base.$printStyle);
			});

			base.updateWidth();
			base.toggleHeaders();
			base.bind();
		};

		base.hideClonedHeader = function(){
			base.$clonedHeader[0].style.display = 'none';
			/*base.$clonedHeader[0].style.overflow = 'hidden';
			base.$clonedHeader[0].style.height = '0px';
			$('th', base.$clonedHeader).css({overflow: 'hidden', height: '0px'});
			$('tr', base.$clonedHeader).css({overflow: 'hidden', height: '0px'});*/
			//base.$clonedHeader[0].style.display = 'none';
			/*base.$clonedHeader[0].style.position = 'fixed';
			base.$clonedHeader[0].style.top = '-999px';*/
		}
		base.showClonedHeader = function(){
			/*base.$clonedHeader[0].style.overflow = '';
			base.$clonedHeader[0].style.height = 'auto';*/
			base.$clonedHeader[0].style.display = '';
		}

		base.destroy = function (){
			base.$el.unbind('destroyed', base.teardown);
			base.teardown();
		};

		base.teardown = function(){
			if (base.isSticky) {
				base.$originalHeader.css('position', 'static');
			}
			$.removeData(base.el, 'plugin_' + name);
			base.unbind();

			base.$clonedHeader.remove();
			base.$originalHeader.removeClass('tableFloatingHeaderOriginal');
			base.$originalHeader.css('visibility', 'visible');
			base.$printStyle.remove();

			base.el = null;
			base.$el = null;
		};

		base.bind = function(){
			base.$scrollableArea.on('scroll.' + name, base.toggleHeaders);
			if (!base.isWindowScrolling) {
				base.$window.on('scroll.' + name + base.id, base.setPositionValues);
				base.$window.on('resize.' + name + base.id, base.toggleHeaders);
			}
			base.$scrollableArea.on('resize.' + name, base.toggleHeaders);
			base.$scrollableArea.on('resize.' + name, base.updateWidth);
		};

		base.unbind = function(){
			// unbind window events by specifying handle so we don't remove too much
			base.$scrollableArea.off('.' + name, base.toggleHeaders);
			if (!base.isWindowScrolling) {
				base.$window.off('.' + name + base.id, base.setPositionValues);
				base.$window.off('.' + name + base.id, base.toggleHeaders);
			}
			base.$scrollableArea.off('.' + name, base.updateWidth);
		};

		base.toggleHeaders = function () {
			if (base.$el) {
				base.$el.each(function () {
					var $this = $(this),
						newLeft,
						newTopOffset = base.isWindowScrolling ? (
									isNaN(base.options.fixedOffset) ?
									base.options.fixedOffset.outerHeight() :
									base.options.fixedOffset
								) :
								base.$scrollableArea.offset().top + (!isNaN(base.options.fixedOffset) ? base.options.fixedOffset : 0),
						offset = $this.offset(),

						scrollTop = base.$scrollableArea.scrollTop() + newTopOffset,
						scrollLeft = base.$scrollableArea.scrollLeft(),

						scrolledPastTop = base.isWindowScrolling ?
								scrollTop > offset.top :
								newTopOffset > offset.top,
						notScrolledPastBottom = function(){	// patch by v6ak: made lazy in order to unlag scrolling in Firefox
							return (base.isWindowScrolling ? scrollTop : 0) <
								(offset.top + $this.height() - base.$originalHeader.height() - (base.isWindowScrolling ? 0 : newTopOffset));
						};

					if (scrolledPastTop && notScrolledPastBottom()) {
						// It should be done at once, in order to prevent ugly effects
						queue(function () {
							newLeft = offset.left - scrollLeft + base.options.leftOffset;
							/*var baseStyle = base.$originalHeader[0].style;
							baseStyle.position = 'fixed';
							baseStyle.marginTop = base.options.marginTop + 50;
							baseStyle.left = newLeft;
							baseStyle.zIndex = 3; // #18: opacity bug*/
							base.$originalHeader.css({
								'position': 'fixed',
								'margin-top': base.options.marginTop,
								'left': newLeft,
								'z-index': 3 // #18: opacity bug
							});
							base.leftOffset = newLeft;
							base.topOffset = newTopOffset;
							var oldIsSticky = base.isSticky;
							base.isSticky = true;
							base.showClonedHeader();
							if (!oldIsSticky) {
								// make sure the width is correct: the user might have resized the browser while in static mode
								base.updateWidth();
								$this.trigger('enabledStickiness.' + name);
							}
							base.setPositionValues();
						});
					} else if (base.isSticky) {
						base.$originalHeader.css('position', 'static');
						base.hideClonedHeader();
						base.resetWidth($('td,th', base.$clonedHeader), $('td,th', base.$originalHeader));
						base.isSticky = false;
						$this.trigger('disabledStickiness.' + name);
					}
				});
			}
		};
		

		base.setPositionValues = function () {
			var winScrollTop = base.$window.scrollTop(),
				winScrollLeft = base.$window.scrollLeft();
			if (!base.isSticky ||
					winScrollTop < 0 || winScrollTop + base.$window.height() > base.$document.height() ||
					winScrollLeft < 0 || winScrollLeft + base.$window.width() > base.$document.width()) {
				return;
			}
			/*var originalHeaderStyle = base.$originalHeader[0].style;
			originalHeaderStyle.top = base.topOffset - (base.isWindowScrolling ? 0 : winScrollTop);
			originalHeaderStyle.left = base.leftOffset - (base.isWindowScrolling ? 0 : winScrollLeft);*/
			base.$originalHeader.css({
				'top': base.topOffset - (base.isWindowScrolling ? 0 : winScrollTop),
				'left': base.leftOffset - (base.isWindowScrolling ? 0 : winScrollLeft)
			});
		};

		base.updateWidth = function () {
			if (!base.isSticky) {
				return;
			}
			// Copy cell widths from clone
			if (!base.$originalHeaderCells) {
				base.$originalHeaderCells = $('th,td', base.$originalHeader);
			}
			if (!base.$clonedHeaderCells) {
				base.$clonedHeaderCells = $('th,td', base.$clonedHeader);
			}
			var cellWidths = base.getWidth(base.$clonedHeaderCells);
			base.setWidth(cellWidths, base.$clonedHeaderCells, base.$originalHeaderCells);

			// Copy row width from whole table
			base.$originalHeader.css('width', base.$originalHeader.width());
			//base.$originalHeader[0].style.width = base.$clonedHeader.width();
		};

		base.getWidth = function ($clonedHeaders) {
			var widths = [];
			$clonedHeaders.each(function (index) {
				var width, $this = $(this);

				if ($this.css('box-sizing') === 'border-box') {
					var boundingClientRect = $this[0].getBoundingClientRect();
					if(boundingClientRect.width) {
						width = boundingClientRect.width; // #39: border-box bug
					} else {
						width = boundingClientRect.right - boundingClientRect.left; // ie8 bug: getBoundingClientRect() does not have a width property
					}
				} else {
					var $origTh = $('th', base.$originalHeader);
					if ($origTh.css('border-collapse') === 'collapse') {
						if (window.getComputedStyle) {
							width = parseFloat(window.getComputedStyle(this, null).width);
						} else {
							// ie8 only
							var leftPadding = parseFloat($this.css('padding-left'));
							var rightPadding = parseFloat($this.css('padding-right'));
							// Needs more investigation - this is assuming constant border around this cell and it's neighbours.
							var border = parseFloat($this.css('border-width'));
							width = $this.outerWidth() - leftPadding - rightPadding - border;
						}
					} else {
						width = $this.width();
					}
				}

				widths[index] = width;
			});
			return widths;
		};

		base.setWidth = function (widths, $clonedHeaders, $origHeaders) {
			$clonedHeaders.each(function (index) {
				var width = widths[index];
				/*var el = $origHeaders.eq(index)[0];
				el.style.minWidth =  width;
				el.style.maxWidth = width;*/
				$origHeaders.eq(index).css({
					'min-width': width,
					'max-width': width
				});
			});
		};

		base.resetWidth = function ($clonedHeaders, $origHeaders) {
			$clonedHeaders.each(function (index) {
				queue(function(){
					var $this = $(this);
					var els = $origHeaders.eq(index)[0].style;
					els.minWidth = $this.css('min-width');
					els.maxWidth = $this.css('max-width');
					/*$origHeaders.eq(index).css({
					 'min-width': $this.css('min-width'),
					 'max-width': $this.css('max-width')
					 });*/
				});
			});
		};

		base.setOptions = function (options) {
			base.options = $.extend({}, defaults, options);
			base.$window = $(base.options.objWindow);
			base.$head = $(base.options.objHead);
			base.$document = $(base.options.objDocument);
			base.$scrollableArea = $(base.options.scrollableArea);
			base.isWindowScrolling = base.$scrollableArea[0] === base.$window[0];
		};

		base.updateOptions = function (options) {
			base.setOptions(options);
			// scrollableArea might have changed
			base.unbind();
			base.bind();
			base.updateWidth();
			base.toggleHeaders();
		};

		// Run initializer
		base.init();
	}

	// A plugin wrapper around the constructor,
	// preventing against multiple instantiations
	$.fn[name] = function ( options ) {
		return this.each(function () {
			var instance = $.data(this, 'plugin_' + name);
			if (instance) {
				if (typeof options === 'string') {
					instance[options].apply(instance);
				} else {
					instance.updateOptions(options);
				}
			} else if(options !== 'destroy') {
				$.data(this, 'plugin_' + name, new Plugin( this, options ));
			}
		});
	};

})(jQuery, window);
