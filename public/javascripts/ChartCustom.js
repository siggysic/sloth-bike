var ctx = document.getElementById("myChart");
var avail = document.getElementById("available");
var out = document.getElementById("outOfOrder");
var borrowOne = document.getElementById("borrowOneDay");
var borrowMTO = document.getElementById("borrowMoreThanOne");

var myChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
        labels: ["พร้อมใช้งาน", "ส่งซ่อม", "ยืมภายในวัน", "ยืมมากกว่า 1 วัน"],
        datasets: [{
            label: '# of Votes',
            data: [avail.value, out.value, borrowOne.value, borrowMTO.value],
            backgroundColor: [
                'rgba(255, 99, 132, 1)',
                'rgba(54, 162, 235, 1)',
                'rgba(255, 206, 86, 1)',
                'rgba(75, 192, 192, 1)'
            ],
            borderColor: [
                'rgba(255,99,132,1)',
                'rgba(54, 162, 235, 1)',
                'rgba(255, 206, 86, 1)',
                'rgba(75, 192, 192, 1)'
            ],
            borderWidth: 1
        }]
    },
    options: {
        maintainAspectRatio: false,
        legend: {
            align: 'center'
        }
    }
});