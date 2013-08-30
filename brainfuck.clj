(defn brainfuck-interpreter [& lines]
	(let [program (str lines)]
		(loop [cells [0N], cells-ptr 0, inst-ptr 0]
			(condp = (get program inst-ptr)
				\>	(let [next-ptr (inc cells-ptr)
							next-cells (if (= next-ptr (count cells)) (conj cells 0N) cells)]
						(recur next-cells next-ptr (inc inst-ptr)))
				\<	(recur cells (dec cells-ptr) (inc inst-ptr))
				\+	(recur (update-in cells [cells-ptr] inc) cells-ptr (inc inst-ptr))
				\-	(recur (update-in cells [cells-ptr] dec) cells-ptr (inc inst-ptr))
				\.	(do
						(print (char (nth cells cells-ptr)))
						(recur cells cells-ptr (inc inst-ptr)))
				\,	(let [ch (.read System/in)]
						(recur (assoc cells cells-ptr ch) cells-ptr (inc inst-ptr)))
				\[	(if (zero? (nth cells cells-ptr))
						(recur cells cells-ptr
							(loop [i (inc inst-ptr) opened 0]
								(condp = (nth program i)
									\[	(recur (inc i) (inc opened))
									\]	(if (zero? opened)
											(inc i)
											(recur (inc i) (dec opened)))
									(recur (inc i) opened))))
						(recur cells cells-ptr (inc inst-ptr)))
			\]	(recur cells cells-ptr
					(loop [i (dec inst-ptr) opened 0]
						(condp = (nth program i)
							\]	(recur (dec i) (inc opened))
							\[	(if (zero? opened)
									i
									(recur (dec i) (dec opened)))
							(recur (dec i) opened))))
				nil cells
				(recur cells cells-ptr (inc inst-ptr))))))
