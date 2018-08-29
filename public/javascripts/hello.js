$(document).ready(function() {

    $('#table-clickable tr').click(function() {
        var href = $(this).find("a").attr("href");
        if(href) {
            window.location = href;
        }
    });

});

$(document).ready(function() {
    var currentId;
    var paymentId;
    $("#paymentTable td button").click(function() {
        currentId = $(this).attr('id');
        $.ajax({
            type: 'GET',
            url: '/api/history/' + currentId,
            success: function(json) {
                var data = json.data;
                paymentId = data.payment.id
                $("#paymentModal .modal-body").html(`
                    <div class="col-md-12 row">
                        <div class="col-md-3">
                            <img src="${data.student.profilePicture}" class="img-thumbnail" alt="Cinque Terre">
                        </div>
                        <div class="col-md-9">
                            <p>${data.student.id}</p>
                            <p>${data.student.firstName} ${data.student.lastName}</p>
                            <p>${data.student.major}</p>
                            <p>Start: ${data.history.borrowDate}</p>
                            <p>To: ${data.history.returnDate}</p>
                            <p>ค่าปรับเกินเวลา: ${data.payment.overtimeFine}</p>
                            <p>ค่าปรับชำรุด: ${data.payment.defectFine}</p>
                            <p>สาเหตุ: ${data.payment.note}</p>
                            <div class="form-group">
                                <label for="fine" class="col-form-label">ค่าอื่นๆ(ถ้ามี): </label>
                                <input type="number" id="fine" name="fine" class="form-control form-control-sm" />
                            </div>
                            <div class="form-group">
                                <label for="note" class="col-form-label">Note</label>
                                <input type="text" id="note" name="note" class="form-control form-control-sm" />
                            </div>
                        </div>
                    </div>
                `);
            }
        });
    });

    $("#submitUpdate").click(function() {
       var data = $("#paymentModal form").serializeArray();

       var json = {};
       $.map(data, function(n, i) {
            json[n['name']] = n['value']
       });
       json.fine = Number(json.fine)
       json.parentId = paymentId
       console.log(json)

       $.ajax({
            type: 'POST',
            url: '/api/payments',
            data: JSON.stringify(json),
            contentType: "application/json",
            async: false,
            success: function(data) {
                document.location.href = "/payments"
            },
            error: function(XMLHttpRequest, textStatus, errorThrown) {
                alert("Error: Can't update");
            }
       })
       WriteCookie();
    });

})
