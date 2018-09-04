var ctx = document.getElementById("myChart");
var avail = document.getElementById("available");
var out = document.getElementById("outOfOrder");
var borrowOne = document.getElementById("borrowOneDay");
var borrowMTO = document.getElementById("borrowMoreThanOne");

var myChart = new Chart(ctx, {
    type: 'horizontalBar',
    data: {
        labels: ["Bike's report"],
        datasets: [
            {
                label: ["พร้อมใช้งาน"],
                data: [avail.value],
                backgroundColor: ['rgba(124, 252, 0, 1)'],
                borderColor: ['rgba(124, 252, 0, 1)'],
                borderWidth: 1
            },
            {
                label: ["ส่งซ่อม"],
                data: [out.value],
                backgroundColor: ['rgba(255, 255, 0, 1)'],
                borderColor: ['rgba(255, 255, 0, 1)'],
                borderWidth: 1
            },
            {
                label: ["ยืมภายในวัน"],
                data: [borrowOne.value],
                backgroundColor: ['rgba(255, 0, 0, 1)'],
                borderColor: ['rgba(255, 0, 0, 1)'],
                borderWidth: 1
            },
            {
                label: ["ยืมมากกว่า 1 วัน"],
                data: [borrowMTO.value],
                backgroundColor: ['rgba(139, 0, 0, 1)'],
                borderColor: ['rgba(139, 0, 0, 1)'],
                borderWidth: 1
            },
        ]
    },
    options: {
        maintainAspectRatio: false,
        legend: {
            align: 'center'
        },
        scales: {
            xAxes: [{
                ticks: {
                    beginAtZero:true,
                    fontFamily: "'Open Sans Bold', sans-serif",
                    fontSize:11
                },
                scaleLabel:{
                    display:false
                },
                gridLines: {
                },
                stacked: true
            }],
            yAxes: [{
                gridLines: {
                    display:false,
                    color: "#fff",
                    zeroLineColor: "#fff",
                    zeroLineWidth: 0
                },
                ticks: {
                    fontFamily: "'Open Sans Bold', sans-serif",
                    fontSize:11
                },
                stacked: true
            }]
        }
        // ,
        // animation: {
        //     onComplete: function () {
        //         var chartInstance = this.chart;
        //         var ctx = chartInstance.ctx;
        //         ctx.textAlign = "left";
        //         ctx.font = "26px Open Sans";
        //         ctx.fillStyle = "#fff";
        //
        //         Chart.helpers.each(this.data.datasets.forEach(function (dataset, i) {
        //             var meta = chartInstance.controller.getDatasetMeta(i);
        //             label: ['พร้อมใช้งาน', 'ส่งซ่อม', 'ยืมภายในวัน', 'ยืมมากกว่า 1 วัน'],
        //
        //                 Chart.helpers.each(meta.data.forEach(function (bar, index) {
        //                     data = dataset.data[index];
        //                     ctx.fillText(data, bar._model.x, bar._model.y - 5);
        //                 }),this)
        //         }),this);
        //     }
        // },
    }
});