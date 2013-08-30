(defn brainfuck-interpreter [& lines]
	(let [program (str lines)
		goto-bracket (fn [same-bracket other-bracket inst-ptr dir] 
			(loop [i (dir inst-ptr) opened 0]
				(condp = (nth program i)
					same-bracket	(recur (dir i) (inc opened))
					other-bracket	(if (zero? opened) i (recur (dir i) (dec opened)))
					(recur (dir i) opened))))]
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
				\[	(recur cells cells-ptr (inc (if (zero? (nth cells cells-ptr))
						(goto-bracket \[ \] inst-ptr inc)
						inst-ptr)))
				\]	(recur cells cells-ptr (goto-bracket \] \[ inst-ptr dec))
				nil cells
				(recur cells cells-ptr (inc inst-ptr))))))
