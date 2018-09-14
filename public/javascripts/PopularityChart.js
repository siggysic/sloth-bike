$(document).ready(function() {
    var ctx = document.getElementById("myChart");
    var myChart;
    getChartData();

    function getUrlVars()
    {
        var vars = [], hash;
        var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
        for(var i = 0; i < hashes.length; i++)
        {
            hash = hashes[i].split('=');
            vars.push(hash[0]);
            vars[hash[0]] = hash[1];
        }
        return vars;
    }

    function getChartData() {
        var params = getUrlVars();
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

