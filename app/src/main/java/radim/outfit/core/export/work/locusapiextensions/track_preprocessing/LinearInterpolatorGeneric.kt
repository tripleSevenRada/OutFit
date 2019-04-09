package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import kotlin.math.roundToLong

// https://discuss.kotlinlang.org/t/how-to-write-generic-functions-for-all-numeric-types/7367
// There’s no completely satisfactory way to write generic functions for all numeric types.
// val double = linearInterpolatorGeneric(1.0, 2.0, 0.5) // returns 1.5
@Suppress("Unchecked_cast")
fun <T> linearInterpolatorGeneric(A: T, B: T, coef: Double): T where T : Number {
    return if (A is Double && B is Double) (A + ((B - A) * coef)) as T
    else if (A is Long && B is Long) (A + ((B - A) * coef).roundToLong()) as T
    else throw UnsupportedOperationException("linearInterpolatorGeneric")
}