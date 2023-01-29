export interface SiPrefix {
  symbol: string;
  multiplier: number;
}

let _siPrefixes: SiPrefix[] = [];

fetch("api/siPrefix")
  .then((response) => response.json())
  .then((data) => (_siPrefixes = data));

export function siPrefixes(): SiPrefix[] | undefined {
  return undefined;
}

export function getSiPrefix(value: number) {
  for (const prefix of _siPrefixes) {
    if (value >= prefix.multiplier) return prefix;
  }
  return _siPrefixes.find((x) => x.multiplier === 1)!;
}
