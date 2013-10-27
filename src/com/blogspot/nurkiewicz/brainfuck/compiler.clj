(ns com.blogspot.nurkiewicz.brainfuck.compiler)

(declare brainfuck-seq-translator)

(defn- loop-end-idx [program]
	(loop [idx 1 open-brackets 0]
		(condp = (nth program idx)
			\]	(if (zero? open-brackets)
					idx
					(recur (inc idx) (dec open-brackets)))
			\[	(recur (inc idx) (inc open-brackets))
				(recur (inc idx) open-brackets))))

(defn- insert-loop-fun-direct [loop-name program code]
	(let [loop-body (->> program (take (loop-end-idx program)) rest)
		loop-body-code (brainfuck-seq-translator loop-body)
		loop-code 
			`(loop [~'state ~'state]
				(if (zero? (nth (:cells ~'state) (:ptr ~'state)))
					~'state
					(recur ~loop-body-code)))
		inner-loop-fun (-> code second (conj `(~loop-name [~'state] ~loop-code)))]
		(assoc code 1 inner-loop-fun)))

(defn- insert-loop-fun [loop-name program code]
	(insert-loop-fun-direct loop-name program 
		(if (= (first code) `letfn) 
			code 
			`[letfn [] ~(apply list code)])))

(defn- brainfuck-seq-translator [program]
	(apply list
		(loop [code `[-> ~'state], program program]
			(condp = (first program)
				\> (recur (conj code `~'move-right) (rest program))
				\< (recur (conj code `~'move-left) (rest program))
				\+ (recur (conj code `~'cell-inc) (rest program))
				\- (recur (conj code `~'cell-dec) (rest program))
				\[ (let [loop-name (gensym "loop")
					loop-body (drop (loop-end-idx program) program)]
					(recur 
						(->> `~loop-name (conj code) (insert-loop-fun loop-name program)) 
						loop-body))
				nil code
				(recur code (rest program))
				))))

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
