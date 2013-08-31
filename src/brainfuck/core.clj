(ns brainfuck.core)

(defn brainfuck-interpreter [& lines]
	(let [program (apply str lines)
		goto-bracket (fn [same-bracket other-bracket ip dir]
			(loop [i (dir ip) opened 0]
				(condp = (nth program i)
					same-bracket	(recur (dir i) (inc opened))
					other-bracket	(if (zero? opened) i (recur (dir i) (dec opened)))
					(recur (dir i) opened))))]
		(loop [cells [0N], cell 0, ip 0]
			(condp = (get program ip)
				\>	(let [next-ptr (inc cell)
							next-cells (if (= next-ptr (count cells)) (conj cells 0N) cells)]
						(recur next-cells next-ptr (inc ip)))
				\<	(recur cells (dec cell) (inc ip))
				\+	(recur (update-in cells [cell] inc) cell (inc ip))
				\-	(recur (update-in cells [cell] dec) cell (inc ip))
				\.	(do
						(print (char (nth cells cell)))
						(recur cells cell (inc ip)))
				\,	(let [ch (.read System/in)]
						(recur (assoc cells cell ch) cell (inc ip)))
				\[	(recur cells cell (inc (if (zero? (nth cells cell))
						(goto-bracket \[ \] ip inc)
						ip)))
				\]	(recur cells cell (goto-bracket \] \[ ip dec))
				nil cells
				(recur cells cell (inc ip))))))
