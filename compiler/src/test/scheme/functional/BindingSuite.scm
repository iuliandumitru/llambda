(define-test "simple let*" (expect 70
	; This is taken from R7RS
	(let ((x 2) (y 3))
	  (let* ((x 7)
			 (z (+ x y)))
		(* z x)))))

(define-test "recursive function definition in lambda body" (expect #t
  ((lambda ()
     (define even?
     (lambda (n)
       (if (zero? n)
       #t
       (odd? (- n 1)))))
     
     (define odd?
     (lambda (n)
       (if (zero? n)
       #f
       (even? (- n 1)))))

     (even? 8)
  ))))

(define-test "recursive mutable function definition in lambda body" (expect #t
  ((lambda ()
     (define even?
     (lambda (n)
       (if (zero? n)
       #t
       (odd? (- n 1)))))
     
     (define odd?
     (lambda (n)
       (if (zero? n)
       #f
       (even? (- n 1)))))
	 
     ; Setting this to a procedure returning true means (even?) will always return true
     (set! odd? (lambda (n) #t))
     (even? 7)
  ))))

(define-test "accessing recursive define before initialization fails" (expect-failure
  ((lambda ()
     (define even?
     (lambda (n)
       (if (zero? n)
       #t
       (odd? (- n 1)))))

     ; This used to be five-is-odd but the inliner would actually resolve this successfuly at compile time at -O 2 
     (define eleven-is-odd (odd? 11))
     
     (define odd?
     (lambda (n)
       (if (zero? n)
       #f
       (even? (- n 1)))))
  ))))

(define-test "hygienic scoped macro binding using let-syntax" (expect (now . outer)
  (define result1 (let-syntax ((given-that (syntax-rules ()
										 ((given-that test stmt1 stmt2 ...)
										  (if test
											(begin stmt1
												   stmt2 ...))))))
	; This is also a s)eaky hygiene test from R7RS
	; "if" is just a normal variable in the below code
	; Overriding it must not interfere with the original report "if" in the macro above once its expanded
	(let ((if #t))
	  (given-that if (set! if 'now))
	  if)))
  
  (define result2 (let ((x 'outer))
	(let-syntax ((m (syntax-rules () ((m) x))))
	  (let ((x 'inner))
		(m)))))
  (cons result1 result2))) 

(define-test "hygienic scoped macro binding using letrec-syntax" (expect 7
   (letrec-syntax
     ((my-or (syntax-rules ()
                           ((my-or) #f)
                           ((my-or e) e)
                           ((my-or e1 e2 ...)
                            (let ((temp e1))
                              (if temp
                                temp
                                (my-or e2 ...)))))))
     (let ((x #f)
           (y 7)
           (temp 8)
           (let odd?)
           (if even?))

       (my-or x
              (let temp)
              (if y)
              y)))))

(define-test "simple letrec*" (expect 5
  (letrec* ((p
              (lambda (x)
                (+ 1 (q (- x 1)))))
            (q
              (lambda (y)
                (if (zero? y)
                  0
                  (+ 1 (p (- y 1))))))
            (x (p 5))
            (y x))
           y)))

(define-test "simple letrec" (expect #t
  (letrec ((even?
             (lambda (n)
               (if (zero? n)
                 #t
                 (odd? (- n 1)))))
           (odd?
             (lambda (n)
               (if (zero? n)
                 #f
                 (even? (- n 1))))))
    (even? 8))))

(define-test "recursive non-lambda definition" (expect #t
  (define self-param (cons #f (lambda () self-param)))
  (eqv? self-param ((cdr self-param)))))

(define-test "simple typed define" (expect 5
  (import (llambda typed))
  (define: num : <numeric> 5)
  num))

(define-test "mutating typed define" (expect 15.0
  (import (llambda typed))
  (define: num : <numeric> 5)
  (set! num 15.0)
  num))

(define-test "typed define with incompatible initialiser fails" (expect-failure
  (import (llambda typed))
  (define: num : <numeric> "not a number")
  num))

(define-test "mutating typed defined with incompatible initialiser fails" (expect-failure
  (import (llambda typed))
  (define: num : <numeric> 5)
  (set! num "not a string")
  num))
