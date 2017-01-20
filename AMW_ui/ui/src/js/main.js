/**
*   Custom JS for AMW
**/

// Display popover messages on hover
$("#popover-info").popover({ trigger: "hover" });

$('.navbar-lower').affix({
  offset: {top: 50}
});

$('.datepicker').datepicker({
  format: "dd.mm.yyyy",
  weekStart: 1,
  autoclose: true
});
