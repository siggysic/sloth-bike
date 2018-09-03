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
                var borrowDate = timeConverter(data.history.borrowDate)
                console.log(data.history.borrowDate)
                var returnDate = timeConverter(data.history.returnDate)
                                console.log(data.history.returnDate)

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
                            <p>Start: ${borrowDate}</p>
                            <p>To: ${returnDate}</p>
                            <p>ค่าปรับเกินเวลา: ${data.payment.overtimeFine} บาท</p>
                            <p>ค่าปรับชำรุด: ${data.payment.defectFine} บาท</p>
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

    function timeConverter(UNIX_timestamp){
      var a = new Date(UNIX_timestamp);
      var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
      var year = a.getFullYear();
      var month = months[a.getMonth()];
      var date = a.getDate();
      var hour = a.getHours();
      var min = a.getMinutes();
      var sec = a.getSeconds();
      var time = date + ' ' + month + ' ' + year + ' ' + hour + ':' + min + ':' + sec ;
      return time;
    }

    $("#submitUpdate").click(function() {
       var data = $("#paymentModal form").serializeArray();

       var json = {};
       $.map(data, function(n, i) {
            json[n['name']] = n['value']
       });
       json.fine = Number(json.fine)
       json.parentId = paymentId
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
