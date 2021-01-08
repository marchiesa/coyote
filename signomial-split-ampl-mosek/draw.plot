set term png
set output "results/graph-comparison-Digex.png"
set title "Digex"
set xlabel "margin"
set ylabel "oblivious performance"
set auto x
set yrange [1:]
plot 'results/Digex-ecmp-gravity-maxcong-margin.txt' using 1:2 with lines title "ospf", 'results/Digex-optimal-dag-gravity-maxcong-margin.txt' using 1:2 with lines title "no-margin-opt", 'results/Digex-signomial-gravity-maxcong-margin.txt' using 1:2 with lines title "signomial-unbounded" , 'results/Digex-margin-gravity-maxcong-margin.txt' using 1:2 with lines title "signomial-bounded"
