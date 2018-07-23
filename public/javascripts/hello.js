$(document).ready(function() {

    $('#table-clickable tr').click(function() {
        var href = $(this).find("a").attr("href");
        if(href) {
            window.location = href;
        }
    });

});

if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");
}

$(document).ready(function() {
    var currentId;
    $("#paymentTable td button").click(function() {
        currentId = $(this).attr('id');
        $.ajax({
            type: 'GET',
            url: '/api/payments/' + currentId,
            success: function(data) {
                console.log(data);
                $("#paymentModal .modal-body").html(`
                    <div class="col-md-12">
                        <p>${data.id}</p>
                        <p>{studentName}</p>
                        <p>{major}</p>
                        <p>Start: {from}</p>
                        <p>To: {to}</p>
                        <p>ค่าปรับเกินเวลา: ${data.overtimeFine}</p>
                        <p>ค่าปรับชำรุด: ${data.defectFine}</p>
                        <div class="form-group">
                            <label for="fine" class="col-form-label">ค่าอื่นๆ(บาท): </label>
                            <input type="number" id="fine" name="fine" class="form-control form-control-sm" />
                        </div>
                        <div class="form-group">
                            <label for="note" class="col-form-label">Note</label>
                            <input type="text" id="note" name="note" class="form-control form-control-sm" />
                        </div>
                    </div>
                `);
            }
        });
    });

    $("#submitUpdate").click(function() {
        console.log("XD");
       var data = $("#paymentModal form").serializeArray();

       var json = {};
       $.map(data, function(n, i) {
            json[n['name']] = n['value']
       });

       json.parentId = currentId

       $.ajax({
            type: 'POST',
            url: '/api/payments',
            success: function(data) {
                console.log("Success");
            }
       })

       console.log(json);

    });

})
