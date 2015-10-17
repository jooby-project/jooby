// This function isn't used anywhere, so
// Rollup excludes it from the bundle...
export function square ( x ) {
  return x * 2;
}

// This function gets included
export function cube ( x ) {
  // rewrite this as `square( x ) * x`
  // and see what happens!
  return x * 3;
}