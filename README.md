# brainfuck interpreter and compiler written in Clojure

## brainfuck in Clojure. Part I: Interpreter

Brainfuck is one of the most popular esoteric programming languages. Writing a Brainfuck interpreter is fun, in contrary to actually using this "language". The syntax is dead simple and semantics are rather clear. Thus writing such interpreter is a good candidate for [Kata](http://codekata.pragprog.com/) session, TDD practice, etc. Using Clojure for the task is slightly more challenging due to inherent impedance mismatch between imperative Brainfuck and functional Clojure. However you will find plenty of existing implementations ([[1]](https://github.com/xumingming/brainfuck/blob/master/src/brainfuck/core.clj), [[2]](https://github.com/DuoSRX/braincloj/blob/master/src/braincloj/core.clj), [[3]](http://softwareactually.blogspot.com/2012/06/three-flavors-of-brainfuck-in-clojure.html), [[4]](http://rosettacode.org/wiki/Execute_Brain****/Clojure)), many of them are less idiomatic as they use atoms to mutate state in-place ([[5]](https://github.com/BlakeWilliams/Clojure-Brainfuck/blob/master/src/brain/fuck.clj), [[6]](https://github.com/joegallo/brainfuck/blob/master/src/brain/fuck.clj), [[7]](https://github.com/bool-/clojure-brainfuck/blob/master/src/anthony/bf/brainfuck.clj), [[8]](https://gist.github.com/omasanori/1495970), [[9]](http://www.reddit.com/r/Clojure/comments/1keokh/brainfuck_interpreter_in_two_tweets/)). 

Let's write a simple, idiomatic brainfuck interpreter ourselves, step by step. It turns out that the transition from mutability to immutability is quite straightforward - rather than mutating state in-place we simply exchange previous state with the new one. In Brainfuck state is represented by `cells` (memory), `cell` (pointer to one of the `cells`, an index within `cells`) and `ip` (*instruction pointer*, an instruction currently being executed):

	(loop [cells [0N], cell 0, ip 0]
		; interpretation
		(recur cells cell (inc ip)))

I don't mutate any of the state variables (actually, I'cant by definition) but in each iteration I produce new set of state variables, discarding the old ones. Typically we will at least increment instruction pointer (to evaluate next instruction in the program) but possibly more. That's pretty much it, in each iteration we read one character of the `program` (sequence of brainfuck opcodes) and proceed with appropriately updated state:

	(loop [cells [0N], cell 0, ip 0]
		(condp = (get program ip)
			\>	(recur cells (inc cell) (inc ip))
			\<	(recur cells (dec cell) (inc ip))
			\+	(recur (update-in cells [cell] inc) cell (inc ip))
			\-	(recur (update-in cells [cell] dec) cell (inc ip))
			; more to come
			(recur cells cell (inc ip))))

This should be self-explanatory - `>` and `<` move `cell` pointer while `+` and `-` incremenet/decrement current cell accordingly. In all cases instruction pointer is incremented in order to execute next instruction during next iteration. So far so good. Code for `>` is actually slightly more complex to achieve infinite growing of `cells` vector but that's irrelevant. Handling loops in brainfuck is more interesting. Every time we encounter opening square bracket we conditionally jump to *corresponding* (*not* first encountered) closing bracket. A little bit of logic is required to handle that:

	(defn brainfuck-interpreter [& lines]
		(let goto-bracket (fn [same-bracket other-bracket ip dir]
				(loop [i (dir ip) opened 0]
					(condp = (nth program i)
						same-bracket	(recur (dir i) (inc opened))
						other-bracket	(if (zero? opened) i (recur (dir i) (dec opened)))
						(recur (dir i) opened))))]
			(loop [cells [0N], cell 0, ip 0]
				(condp = (get program ip)
					\[	(recur cells cell (inc (if (zero? (nth cells cell))
							(goto-bracket \[ \] ip inc)
							ip)))
					\]	(recur cells cell (goto-bracket \] \[ ip dec))
					;...
					nil cells
					(recur cells cell (inc ip))))))

Opening bracket jumps to corresponding closing bracket if current cell is zero and proceeds to next instruction otherwise. Closing bracket jumps unconditionally to corresponding opening bracket. Think of them as nested `while` loops. Guess what, we just implemented brainfuck interpreter in functional language without mutating state, at all! The full source code follows, including impure I/O operations and all supporting code:

	(ns com.blogspot.nurkiewicz.brainfuck.interpreter)
	
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

Using this interpreter is quite simple. It terminates when it encounters end of the program. `brainfuck-interpreter` returns state as it was upon termination to allow easier unit testing. This project is available on GitHub, but it was merely a warm-up. In the next article we shall write a brainfuck **compiler** in Clojure. In 100 lines of code. Stay tuned!

---

# brainfuck in Clojure. Part II: compiler

Last time we developed [brainfuck interpreter in Clojure](http://nurkiewicz.blogspot.com/2013/10/brainfuck-in-clojure-part-i-interpreter.html). This time we will write a compiler. Compilation has two advantages over interpretation: the resulting program tends to be faster and source program is lost/obscured in binary. It turns out that a [brainfuck](http://en.wikipedia.org/wiki/Brainfuck) compiler (to any assembly/bytecode) is not really that complex - brainfuck is very low level and similar to typical CPU architectures (chunk of mutable memory, modified one cell at a time). Thus we will go for something slightly different. Instead of producing JVM bytecode (which some [already did](https://github.com/joegallo/brainfuck/blob/master/src/brain/fuck.clj)) we shall write a Clojure macro that will generate code equivalent to any brainfuck program. In other words we will produce Clojure source equivalent to brainfuck source - at compile time.

This task is actually more challenging because idiomatic Clojure is much different from idiomatic brainfuck (if such thing as "*idiomatic brainfuck*" ever existed). In essence every brainfuck program is a sequence of steps, each mutating state (or producing new state based on the current one). For example (please refer to [brainfuck language overview](http://esolangs.org/wiki/Brainfuck#Language_overview) if you haven't yet, there are just 8 commands) the translation from "`++>-<`" in brainfuck to Clojure might look like this:

    (let [state {:ptr 0, :cells [0N]}]
      (-> state 
        cell-inc 
        cell-inc
        move-right
        cell-dec
        )

First we define immutable `state` (an array of `cells` with one item and a `ptr` (index) to the current cell) and then apply a sequence of transformations on top of it. Each transformation (function yet to be defined) yields new state. The `->` macro is a syntactic sugar, more readable than:

        (cell-dec
          (move-right
            (cell-inc
              (cell-inc state))

OK, so let's define all these functions:

	(let [state {:ptr 0, :cells [0N]}
	    cell-inc (fn [state] (update-in state [:cells (:ptr state)] inc))
	    cell-dec (fn [state] (update-in state [:cells (:ptr state)] dec))
	    move-right (fn [state] (update-in state [:ptr] inc))]
	    move-left  (fn [state] (update-in state [:ptr] dec))]
	  (-> state 
	    cell-inc 
	    cell-inc
	    move-right
	    cell-dec
	    ))

`move-right` is actually more complex because it has to grow `cells` when needed but it's irrelevant. With these helper functions it's easy to translate any brainfuck program into Clojure - simply by replacing `+`, `-`, `>` and `<` operators with corresponding functions. Well, we aren't quite there yet. In order to be [Turing complete](http://en.wikipedia.org/wiki/Turing_completeness) needs some form of conditional statement. brainfuck has two conditional jump instructions, `[` and `]`. For our purposes we can treat each pair of square brackets as a single instruction (conceptually it is a `while` loop). So for example `++[>+<-]>` has four instructions:

    (let [state {:ptr 0, :cells [0N]}]
      (-> state 
        cell-inc 
        cell-inc
        loop-nested  ;[>+<-]
        move-right
        )
 
`loop-nested` is a generated function that encapsulates instructions inside square brackets. Such a loop terminates when it encounters `0` at current cell:

	(let
	    [state {:ptr 0, :cells [0N]}
	    (letfn [
	        (loop-nested [state]
	          (loop [state state]
	            (if (zero? (nth (:cells state) (:ptr state)))
	              state
	              (recur
	                (-> state
	                    move-right
	                    cell-inc
	                    move-left
	                    cell-dec)))))]
	    (-> state 
	        cell-inc 
	        cell-inc
	        loop-nested
	        move-right)))

Look carefully! The program starts in the bottom. When it reaches `loop-nested` function (state transformation) it enters nested loop defined above. The loop first checks current cell - if it's zero, present `state` is returned. Otherwise a sequence of `state` transformations defined within nested loop are executed. Once they are all performed with call `recur` in order to start subsequent iteration. Sooner or later `loop-nested` exits and `move-right` (last line above) will execute.

Of course we can nest loops just like in any other programming language, for example: `>+>+++[-<[-<+++++>]<++[->+<]>>]<` is probably the shortest known brainfuck program that generates... 187 constant. You can see outer loop enclosing two nested loops. The equivalent Clojure code we would like to generate looks like that:

    (let
      [state {:ptr 0, :cells [0N]}]
          (letfn [
            (loop1279 [state]   ; [-<[-<+++++>]<++[->+<]>>]
              (loop [state state]
                (if (zero? (nth (:cells state) (:ptr state)))
                  state
                  (recur
                    (letfn [
                        (loop1280 [state]   ; [-<+++++>]
                          (loop [state state]
                            (if (zero? (nth (:cells state) (:ptr state)))
                              state
                              (recur
                                (-> state   ; -<+++++>
                                  cell-dec move-left cell-inc cell-inc cell-inc cell-inc cell-inc move-right)))))
                        (loop1281 [state]   ; [->+<]
                          (loop [state state]
                            (if (zero? (nth (:cells state) (:ptr state)))
                              state
                              (recur
                                (-> state   ; ->+<
                                  cell-dec move-right cell-inc move-left)))))]
                      (-> state   ; -<[...]<++[...]>>
                        cell-dec move-left loop1280 move-left cell-inc cell-inc loop1281 move-right move-right))))))]
        (-> state   ;  >+>+++[...]<
          move-right cell-inc move-right cell-inc cell-inc cell-inc loop1279 move-left))) 

I left comments to guide you which parts correspond to which pieces of brainfuck. Start reading from the very bottom. I guess now we can fully appreciate the conciseness of brainfuck. OK, just joking.

---

Right, so we see how brainfuck can be translated into Clojure. Let's implement such a translator (which I called a *compiler* in the title since it looks better). It might seem complex (especially after seeing code sample above) but the whole translator [fits on one screen](https://github.com/nurkiewicz/brainfuck.clj/blob/master/src/com/blogspot/nurkiewicz/brainfuck/compiler.clj)!

The implementation consists of two main parts - generating code for a block of brainfuck source and injecting function for nested loop. The first part simplified for clarity:

    (defn- translate-block [brainfuck-source]
      (apply list
        (loop [code [`letfn [] `[-> ~'state]], program brainfuck-source]
          (condp = (first program)
            \> (recur (append-cmd code `~'move-right) (rest program))
            \< (recur (append-cmd code `~'move-left) (rest program))
            \+ (recur (append-cmd code `~'cell-inc) (rest program))
            \- (recur (append-cmd code `~'cell-dec) (rest program))
            \[ (let [loop-name (gensym "loop")]
                  (recur 
                    (insert-loop-fun loop-name program code)
                    source-after-loop))
            nil code
            (recur code (rest program))))))

Observe how we iterate over character of brainfuck source and append appropriate commands to Clojure `code` being built (initially set to `(letfn [] ())`). Opening square bracket (`[`) appends auto-generated loop in `insert-loop-fun` function:

    (defn- insert-loop-fun [loop-name brainfuck-source code]
      (let [loop-body "..." 
        loop-body-code (translate-block loop-body)
        loop-code 
          `(loop [~'state ~'state]
            (if (zero? (nth (:cells ~'state) (:ptr ~'state)))
              ~'state
              (recur ~loop-body-code)))]
        `(~loop-name [~'state] ~loop-code)))

Code above is also simplified for readability. Two important steps are performed: generating code for loop body using recursive call to `translate-block` and wrapping final Clojure code with a loop template. Whole, working source code is [available on GitHub](https://github.com/nurkiewicz/brainfuck.clj/blob/master/src/com/blogspot/nurkiewicz/brainfuck/compiler.clj). Let's take this macro for a test drive. Notice that we no longer need to escape brainfuck code as a string, we can place it *directly* in Clojure!

    (is (= 
      (brainfuck +>-<+) 
      {:ptr 0 :cells [2 -1]}))

    (is (= 
      (brainfuck >>+>>-) 
      {:ptr 4 :cells [0 0 1 0 -1]}))

    (is (= 
      (brainfuck 
          >+>+++[
            -<[
              -<+++++>
            ]
            <++[
              ->+<
            ]
          >>
          ]
        <
      ) 
      {:ptr 1 :cells [0 187 0]}))

As you can see invoking `brainfuck` macro yields final state of the program. I/O is not implemented but easy to add. 

To recap: we managed to build a Clojure program in less than 60 lines of code that translates any brainfuck source into valid Clojure source. Later Clojure compiler turns this into JVM bytecode. Source code for both [interpreter](https://github.com/nurkiewicz/brainfuck.clj/blob/master/src/com/blogspot/nurkiewicz/brainfuck/interpreter.clj) and [compiler](https://github.com/nurkiewicz/brainfuck.clj/blob/master/src/com/blogspot/nurkiewicz/brainfuck/compiler.clj) (plus [test cases](https://github.com/nurkiewicz/brainfuck.clj/tree/master/test/com/blogspot/nurkiewicz/brainfuck)) is [available on GitHub](https://github.com/nurkiewicz/brainfuck.clj). 

---

# License

Copyright © 2013 Tomasz Nurkiewicz

Distributed under the Eclipse Public License, the same as Clojure.
