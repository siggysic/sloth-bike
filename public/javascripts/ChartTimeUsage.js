var ctx = document.getElementById("myChart");
var one = document.getElementById("one");
var two = document.getElementById("two");
var three = document.getElementById("three");
var four = document.getElementById("four");
var five = document.getElementById("five");

var myChart = new Chart(ctx, {
    type: 'bar',
    data: {
        labels: ["0-3 ชม.", "3-6 ชม.", "6-9 ชม.", "9-24 ชม.", "24++"],
        datasets: [
            {
                label: ["จำนวนที่ยืม (ครั้ง)"],
                data: [one.value, two.value, three.value, four.value, five.value],
                backgroundColor: ['#FFFF00', '#FFA500', '#FF7F50', '#FF6347', '#FF4500'],
                borderWidth: 1
            }
        ]
    },
    options: {
        maintainAspectRatio: false,
        legend: {
            align: 'center',
            display: false
        },
        title: {
          display: true,
          text: 'กราฟแสดงระยะการใช้งานของนักศึกษา (ชม.)'
        }

    }
});