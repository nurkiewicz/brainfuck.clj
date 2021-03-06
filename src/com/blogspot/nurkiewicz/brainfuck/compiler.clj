(ns com.blogspot.nurkiewicz.brainfuck.compiler)

(declare translate-block)

(defn- loop-end-idx [program]
	(loop [idx 1 open-brackets 0]
		(condp = (nth program idx)
			\]	(if (zero? open-brackets)
					idx
					(recur (inc idx) (dec open-brackets)))
			\[	(recur (inc idx) (inc open-brackets))
				(recur (inc idx) open-brackets))))

(defn- insert-loop-fun [loop-name brainfuck-source code]
	(let [loop-body (->> brainfuck-source (take (loop-end-idx brainfuck-source)) rest)
		loop-body-code (translate-block loop-body)
		loop-code 
			`(loop [~'state ~'state]
				(if (zero? (nth (:cells ~'state) (:ptr ~'state)))
					~'state
					(recur ~loop-body-code)))
		inner-loop-fun (-> code second (conj `(~loop-name [~'state] ~loop-code)))]
		(assoc code 1 inner-loop-fun)))

(defn- append-cmd [code cmd] (update-in code [2] #(conj % cmd)))

(defn- optimize [[_ inner-loops block :as code]]
	(if (empty? inner-loops) block code))

(defn- translate-block [brainfuck-source]
	(apply list
		(loop [code [`letfn [] `[-> ~'state]], program brainfuck-source]
			(condp = (first program)
				\> (recur (append-cmd code `~'move-right) (rest program))
				\< (recur (append-cmd code `~'move-left) (rest program))
				\+ (recur (append-cmd code `~'cell-inc) (rest program))
				\- (recur (append-cmd code `~'cell-dec) (rest program))
				\[ (let [loop-name (gensym "loop")
						source-after-loop (drop (loop-end-idx program) program)]
					(recur 
						(->> `~loop-name (append-cmd code) (insert-loop-fun loop-name program)) 
						source-after-loop))
				nil (-> code (update-in [2] #(apply list %)) optimize)
				(recur code (rest program))))))

(defmacro brainfuck [& instructions]
	(concat
		`(let [~'state {:cells [0N], :ptr 0}
			~'cell-inc (fn [~'state] (update-in ~'state [:cells (:ptr ~'state)] inc))
			~'cell-dec (fn [~'state] (update-in ~'state [:cells (:ptr ~'state)] dec))
			~'move-right (fn [~'state] 
				(let [{:keys [~'cells ~'ptr]} ~'state]
					(if (= (inc ~'ptr) (count ~'cells))
						(assoc ~'state :ptr (inc ~'ptr) :cells (conj ~'cells 0N))
						(assoc ~'state :ptr (inc ~'ptr)))))
			~'move-left (fn [~'state] (update-in ~'state [:ptr] dec))])
		(list (translate-block (str instructions)))))
