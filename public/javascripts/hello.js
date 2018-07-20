$(document).ready(function() {

    $('#table-clickable tr').click(function() {
        var href = $(this).find("a").attr("href");
        if(href) {
            window.location = href;
        }
    });

});

// function doRequest(method, formId, url) {
//   var form = $('#' + formId)
//   $.ajax({
//     type : method,
//     url : url,
//     data : form.serialize(),
//     success : function(data) {
//       console.log("SUCCESS")
//     },
//     error : function(error) {
//       console.log("ERROR")
//     }
//   });
// }

// document.addEventListener('DOMContentLoaded', function () {
//     document.querySelector('#containner tbody tr').addEventListener('click', clickHandler);
// });
//
// function clickHandler() {
//     window.location = $(this).data("href");
// }