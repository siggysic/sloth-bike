$(document).ready(function() {
    var ctx = document.getElementById("myChart");
    var myChart;
    getChartData();

    function getParameterByName(name, url)
    {
        if (!url) url = window.location.href;
        name = name.replace(/[\[\]]/g, '\\$&');
        var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, ' '));
    }

    function getChartData() {
        var startDate = getParameterByName('startDate');
        var endDate = getParameterByName('endDate');
        var params;
        if (startDate != null && endDate != null) {
            params = {
                startDate: startDate,
                endDate: endDate
            }
        } else if (startDate != null && endDate == null) {
             params = {
                startDate: startDate
             }
        } else if (startDate == null && endDate != null) {
             params = {
                endDate: endDate
             }
        } else {
            params = {}
        }
        $.ajax({
            type: "GET",
            url: '/api/reports/popularity',
            data: params,
            success: function(json) {
                var data = json.data;
                var colors = ['rgb(211, 14, 14)', 'rgb(44, 155, 0)', 'rgb(252, 237, 25)']
                var dSet = [];
                var l = [];
                var da = [];
                var bgColor = [];
                data.forEach(function(d, index) {
                    dSet.push({
                        label: [d['range']],
                        data: [d['count']],
                        backgroundColor: ['rgba(124, 252, 0, 1)'],
                        borderColor: ['rgba(124, 252, 0, 1)'],
                        borderWidth: 1
                    });
                    l.push(d['range']);
                    da.push(d['count']);
                    bgColor.push(colors[index%3]);
                })
                var myChart = new Chart(ctx, {
                    type: 'bar',
                    data: {
                        labels: l,
                        datasets: [{
                            data: da,
                            backgroundColor: bgColor,
                            labels: []
                        }]
                    },
                    options: {
                        title: {
                            display: true,
                            text: 'Popularity'
                        },
                        legend: {
                            display: false
                        },
                        scales: {
                            yAxes: [{
                                stacked: true,
                                ticks: {
                                    beginAtZero: true
                                }
                            }],
                            xAxes: [{
                                stacked: true,
                                ticks: {
                                    beginAtZero: false
                                }
                            }]
                        }
                    }
                });
            }
        })
    }
});

