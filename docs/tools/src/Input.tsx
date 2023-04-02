import { Dispatch, SetStateAction } from "react";

export function round(n: number, digits: number) {
    const factor = Math.pow(10, digits);
    return Math.round(n * factor) / factor;
}

// This type returns all keys that have a value of type string
type KeyOfType<T, ValueType> = {
    // for all keys in T
    [K in keyof T]:

    // if the value of this key is a string, keep it. Else, discard it
    T[K] extends ValueType ? K : never

    // Get the union type of the remaining values.
}[keyof T];

interface UnitValue<TUnit> {
    value: number
    unit: TUnit
}

interface UnitInfo {
    label: string,
    factor: number
}

type StateAccess<T> = [T, Dispatch<SetStateAction<T>>, () => void];

function lowerCamelToUpperSpace(input: string) {
    if (input === "")
        return "";
    const tmp = input.replace(/([a-z])([A-Z])/g, '$1 $2')
    return tmp.charAt(0).toUpperCase() + tmp.slice(1);
}


interface UnitInputArgs<T, TUnit extends string, K extends KeyOfType<T, UnitValue<TUnit>>> {
    stateAccess: StateAccess<T>,
    label?: string,
    k: K,
    className?: string,
    units: { [key in TUnit]: UnitInfo }
}
function UnitInput<T, TUnit extends string, K extends KeyOfType<T, UnitValue<TUnit>> & string>({ stateAccess: [state, setState], label, k, units, className }: UnitInputArgs<T, TUnit, K>) {
    const value: UnitValue<TUnit> = state[k] as any;
    const factor = units[value.unit].factor;

    return <div className={className}>
        <label className="form-label">{lowerCamelToUpperSpace(label ?? k)}</label>
        <div className="input-group">
            <input type="number" className="form-control" value={round(value.value / factor, 3)}
                onChange={e => setState(s => {
                    return ({ ...s, [k]: { value: parseFloat(e.target.value) * factor, unit: value.unit } });
                })} />

            <select className="form-select" value={value.unit} onChange={e => setState(s => {
                const newUnit: TUnit = e.target.value as any;
                return ({ ...s, [k]: { value: value.value * units[newUnit].factor / factor, unit: newUnit } });
            })} style={{ flexGrow: 0, width: '7rem' }}>
                {Object.entries(units as { [key: string]: UnitInfo }).sort(([, a], [, b]) => a.factor - b.factor).map(([key, value]) =>
                    <option key={key} value={key}>{value.label}</option>)}
            </select>

        </div>
    </div>;
}

export function NumberInput<T, K extends KeyOfType<T, number> & string>({ stateAccess: [state, setState], label, k, unit, className }:
    { stateAccess: StateAccess<T>, label?: string, k: K, unit?: string, className?: string }) {
    const value: number = state[k] as any;

    return <div className={className}>
        <label className="form-label">{lowerCamelToUpperSpace(label ?? k)}</label>
        <div className="input-group">
            <input type="number" className="form-control" value={value}
                onChange={e => setState(s => {
                    return ({ ...s, [k]: parseFloat(e.target.value) });
                })} />
            {unit === undefined ? null :
                <span className="input-group-text">{unit}</span>
            }
        </div>
    </div>;
}

export function formatValue(value: number, units: { [key: string]: UnitInfo }) {
    const sortedInfos = Object.values(units).sort((a, b) => b.factor - a.factor);
    let chosenInfo = sortedInfos[sortedInfos.length - 1];
    for (const info of sortedInfos) {
        if (info.factor <= value) {
            chosenInfo = info;
            break;
        }
    }
    return round(value / chosenInfo.factor, 3) + ' ' + chosenInfo.label;

}

export const resistanceUnits = {
    'Ohm': { label: "\u2126", factor: 1 },
    'kOhm': { label: "k\u2126", factor: 1e3 },
}
type ResistanceUnit = keyof typeof resistanceUnits;
export type ResistanceValue = UnitValue<ResistanceUnit>
export function ResistanceInput<T, K extends KeyOfType<T, ResistanceValue> & string>(args: Omit<UnitInputArgs<T, ResistanceUnit, K>, 'units'>) {
    return UnitInput<T, ResistanceUnit, K>({ ...args, units: resistanceUnits });
}

export const powerUnits = {
    'mW': { label: "mW", factor: 1e-3 },
    'W': { label: "W", factor: 1 },
}

export const frequencyUnits = {
    'Hz': { label: "Hz", factor: 1 },
    'kHz': { label: "kHz", factor: 1e3 },
    'MHz': { label: "MHz", factor: 1e6 },
}
type FrequencyUnit = keyof typeof frequencyUnits;
export type FrequencyValue = UnitValue<FrequencyUnit>
export function FrequencyInput<T, K extends KeyOfType<T, FrequencyValue> & string>(args: Omit<UnitInputArgs<T, FrequencyUnit, K>, 'units'>) {
    return UnitInput<T, FrequencyUnit, K>({ ...args, units: frequencyUnits });
}


export const capacitanceUnits = {
    F: { label: 'F', factor: 1 },
    mF: { label: 'mF', factor: 1e-3 },
    uF: { label: '\u00b5F', factor: 1e-6 },
    nF: { label: 'nF', factor: 1e-9 },
    pF: { label: 'pF', factor: 1e-12 },
};

type CapacitanceUnit = keyof typeof capacitanceUnits;
export type CapacitanceValue = UnitValue<CapacitanceUnit>
export function CapacitanceInput<T, K extends KeyOfType<T, CapacitanceValue> & string>(args: Omit<UnitInputArgs<T, CapacitanceUnit, K>, 'units'>) {
    return UnitInput<T, CapacitanceUnit, K>({ ...args, units: capacitanceUnits });
}

export const inductanceUnits = {
    H: { label: 'H', factor: 1 },
    mH: { label: 'mH', factor: 1e-3 },
    uH: { label: '\u00b5H', factor: 1e-6 },
    nH: { label: 'nH', factor: 1e-9 },
};
type InductanceUnit = keyof typeof inductanceUnits;
export type InductanceValue = UnitValue<InductanceUnit>
export function InductanceInput<T, K extends KeyOfType<T, InductanceValue> & string>(args: Omit<UnitInputArgs<T, InductanceUnit, K>, 'units'>) {
    return UnitInput<T, InductanceUnit, K>({ ...args, label: args.label, units: inductanceUnits });
}

export const timeUnits = {
    s: { label: 's', factor: 1 },
    ms: { label: 'ms', factor: 1e-3 },
    us: { label: '\u00b5s', factor: 1e-6 },
    ns: { label: 'ns', factor: 1e-9 },
};
type TimeUnit = keyof typeof timeUnits;
type TimeValue = UnitValue<TimeUnit>
export function TimeInput<T, K extends KeyOfType<T, TimeValue> & string>(args: Omit<UnitInputArgs<T, TimeUnit, K>, 'units'>) {
    return UnitInput<T, TimeUnit, K>({ ...args, units: timeUnits });
}

export const voltageUnits = {
    V: { label: 'V', factor: 1 },
    mV: { label: 'mV', factor: 1e-3 },
};
type VoltageUnit = keyof typeof voltageUnits;
export type VoltageValue = UnitValue<VoltageUnit>
export function VoltageInput<T, K extends KeyOfType<T, VoltageValue> & string>(args: Omit<UnitInputArgs<T, VoltageUnit, K>, 'units'>) {
    return UnitInput<T, VoltageUnit, K>({ ...args, units: voltageUnits });
}

export const currentUnits = {
    A: { label: 'A', factor: 1 },
    mA: { label: 'mA', factor: 1e-3 },
};
type CurrentUnit = keyof typeof currentUnits;
export type CurrentValue = UnitValue<CurrentUnit>
export function CurrentInput<T, K extends KeyOfType<T, CurrentValue> & string>(args: Omit<UnitInputArgs<T, CurrentUnit, K>, 'units'>) {
    return UnitInput<T, CurrentUnit, K>({ ...args, units: currentUnits });
}


function clamp(value: number, min?: number, max?: number) {
    if (min !== undefined && value < min)
        return min;
    if (max !== undefined && value > max)
        return max;
    return value
}
export function RangeIndicator(props:
    { value: number, min: number, minWarning?: string, max: number, maxWarning?: string, log?: boolean }
    & ({ units: { [key: string]: UnitInfo } } | { format: (value: number) => string })
) {
    var width: number;
    // var middle: number;
    if (props.log === true) {
        width = 100 * (Math.log(props.value) - Math.log(props.min)) / (Math.log(props.max) - Math.log(props.min));
        // middle = Math.exp(Math.log(props.min) + (Math.log(props.max) - Math.log(props.min)) / 2);
    }
    else {
        width = 100 * (props.value - props.min) / (props.max - props.min);
        // middle = props.min + (props.max - props.min) / 2;
    }

    let format: (value: number) => string;
    if ('units' in props) {
        format = x => formatValue(x, props.units);
    }
    else
        format = props.format;
    return <>
        <div style={{ display: 'flex', paddingBottom: '1em', alignItems: 'stretch', flexDirection: 'column', marginBottom: '1rem' }}>
            <div className="progress">
                <div className="progress-bar" role="progressbar" style={{ width: clamp(width, 0, 100) + '%' }}></div>
            </div>
            <div style={{ position: 'relative' }}>
                <span style={{ position: 'absolute', left: 0 }}>{format(props.min)}</span>
                {/* <span style={{ position: 'absolute', left: '50%' }}>{formatValue(middle, props.units)}</span> */}
                <span style={{ position: 'absolute', right: 0 }}>{format(props.max)}</span>
            </div>
        </div>
        {width < 0 && props.minWarning !== undefined ? <div className="text-bg-warning p-3">{props.minWarning}</div> : null}
        {width > 100 && props.maxWarning !== undefined ? <div className="text-bg-warning p-3">{props.maxWarning}</div> : null}
    </>
}

