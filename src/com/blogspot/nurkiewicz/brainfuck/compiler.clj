(ns com.blogspot.nurkiewicz.brainfuck.compiler)

(defn- brainfuck-seq-translator [program]
	(reverse
		(reduce 
			(fn [code next-instr] 
				(condp = next-instr
					\>	(cons `~'move-right code)
					\<	(cons `~'move-left code)
					\+	(cons `~'cell-inc code)
					\-	(cons `~'cell-dec code)
					code))
			`(~'state ->)
			program)))
			
(defn brainfuck-translator [& lines]
	(concat
		`(let [~'state {:cells [0N], :ptr 0}
			~'make-update-cell-fn (fn [~'fun]
				(fn [~'state] (update-in ~'state [:cells] #(update-in % [(:ptr ~'state)] ~'fun))))
			~'cell-inc (~'make-update-cell-fn inc)
			~'cell-dec (~'make-update-cell-fn dec)
			~'move-right (fn [~'state] 
				(let [{:keys [~'cells ~'ptr]} ~'state]
					(if (= (inc ~'ptr) (count ~'cells))
						(assoc ~'state :ptr (inc ~'ptr) :cells (conj ~'cells 0N))
						(assoc ~'state :ptr (inc ~'ptr)))))
			~'move-left (fn [~'state] (update-in ~'state [:ptr] dec))])
		(list (brainfuck-seq-translator (apply str lines)))))
