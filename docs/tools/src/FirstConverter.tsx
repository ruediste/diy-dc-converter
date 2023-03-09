import { Dispatch, SetStateAction } from "react";
import useLocalStorage from "./useLocalStorage"



function round(n: number, digits: number) {
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

type StateAccess<T> = [T, Dispatch<SetStateAction<T>>];


interface UnitInputArgs<T, TUnit extends string, K extends KeyOfType<T, UnitValue<TUnit>>> {
    stateAccess: StateAccess<T>, label?: string, k: K, units: { [key in TUnit]: UnitInfo }
}
function UnitInput<T, TUnit extends string, K extends KeyOfType<T, UnitValue<TUnit>> & string>({ stateAccess: [state, setState], label, k, units }: UnitInputArgs<T, TUnit, K>) {
    const value: UnitValue<TUnit> = state[k] as any;
    const factor = units[value.unit].factor;
    return <div className="mb-3">
        <label className="form-label">{label ?? k}</label>
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

function formatValue(value: number, units: { [key: string]: UnitInfo }) {
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

const frequencyUnits = {
    'Hz': { label: "Hz", factor: 1 },
    'kHz': { label: "kHz", factor: 1e3 },
}
type FrequencyUnit = keyof typeof frequencyUnits;
type FrequencyValue = UnitValue<FrequencyUnit>
export function FrequencyInput<T, K extends KeyOfType<T, FrequencyValue> & string>(args: { stateAccess: StateAccess<T>, k: K, label?: string }) {
    return UnitInput<T, FrequencyUnit, K>({ ...args, label: args.label, units: frequencyUnits });
}


const inductanceUnits = {
    H: { label: 'H', factor: 1 },
    mH: { label: 'mH', factor: 1e-3 },
    uH: { label: '\u00b5H', factor: 1e-6 },
    nH: { label: 'nH', factor: 1e-9 },
};
type InductanceUnit = keyof typeof inductanceUnits;
type InductanceValue = UnitValue<InductanceUnit>
function InductanceInput<T, K extends KeyOfType<T, InductanceValue> & string>(args: { stateAccess: StateAccess<T>, k: K, label?: string }) {
    return UnitInput<T, InductanceUnit, K>({ ...args, label: args.label, units: inductanceUnits });
}

const timeUnits = {
    s: { label: 's', factor: 1 },
    ms: { label: 'ms', factor: 1e-3 },
    us: { label: '\u00b5s', factor: 1e-6 },
    ns: { label: 'ns', factor: 1e-9 },
};
type TimeUnit = keyof typeof timeUnits;
type TimeValue = UnitValue<TimeUnit>
export function TimeInput<T, K extends KeyOfType<T, TimeValue> & string>(args: { stateAccess: StateAccess<T>, k: K, label?: string }) {
    return UnitInput<T, TimeUnit, K>({ ...args, label: args.label, units: timeUnits });
}

const voltageUnits = {
    V: { label: 'V', factor: 1 },
    mV: { label: 'mV', factor: 1e-3 },
};
type VoltageUnit = keyof typeof voltageUnits;
type VoltageValue = UnitValue<VoltageUnit>
function VoltageInput<T, K extends KeyOfType<T, VoltageValue> & string>(args: { stateAccess: StateAccess<T>, k: K, label?: string }) {
    return UnitInput<T, VoltageUnit, K>({ ...args, label: args.label, units: voltageUnits });
}

const currentUnits = {
    A: { label: 'A', factor: 1 },
    mA: { label: 'mA', factor: 1e-3 },
};
type CurrentUnit = keyof typeof currentUnits;
type CurrentValue = UnitValue<CurrentUnit>
function CurrentInput<T, K extends KeyOfType<T, CurrentValue> & string>(args: { stateAccess: StateAccess<T>, k: K, label?: string }) {
    return UnitInput<T, CurrentUnit, K>({ ...args, label: args.label, units: currentUnits });
}


function clamp(value: number, min?: number, max?: number) {
    if (min !== undefined && value < min)
        return min;
    if (max !== undefined && value > max)
        return max;
    return value
}
function RangeIndicator(props: { value: number, min: number, minWarning?: string, max: number, maxWarning?: string, log?: boolean, units: { [key: string]: UnitInfo } }) {
    var width: number;
    var middle: number;
    if (props.log === true) {
        width = 100 * (Math.log(props.value) - Math.log(props.min)) / (Math.log(props.max) - Math.log(props.min));
        middle = Math.exp(Math.log(props.min) + (Math.log(props.max) - Math.log(props.min)) / 2);
    }
    else {
        width = 100 * (props.value - props.min) / (props.max - props.min);
        middle = props.min + (props.max - props.min) / 2;
    }
    return <>
        <div style={{ display: 'flex', paddingBottom: '1em', alignItems: 'stretch', flexDirection: 'column', marginBottom: '1rem' }}>
            <div className="progress">
                <div className="progress-bar" role="progressbar" style={{ width: clamp(width, 0, 100) + '%' }}></div>
            </div>
            <div style={{ position: 'relative' }}>
                <span style={{ position: 'absolute', left: 0 }}>{formatValue(props.min, props.units)}</span>
                <span style={{ position: 'absolute', left: '50%' }}>{formatValue(middle, props.units)}</span>
                <span style={{ position: 'absolute', right: 0 }}>{formatValue(props.max, props.units)}</span>
            </div>
        </div>
        {width < 0 && props.minWarning !== undefined ? <div className="text-bg-warning p-3">{props.minWarning}</div> : null}
        {width > 100 && props.maxWarning !== undefined ? <div className="text-bg-warning p-3">{props.maxWarning}</div> : null}
    </>
}

interface State {
    inputVoltage: VoltageValue
    outputVoltage: VoltageValue
    inductance: InductanceValue
    // switchingFrequency: FrequencyValue
    idleFraction: number
    loadCurrent: CurrentValue
}


export function FirstConverter() {
    const stateAccess = useLocalStorage<State>('firstConverter', {
        inputVoltage: { value: 5, unit: 'V' },
        outputVoltage: { value: 12, unit: 'V' },
        inductance: { value: 100e-6, unit: 'uH' },
        idleFraction: 0.1,
        loadCurrent: { value: 0.1, unit: 'mA' }
    })
    const [state] = stateAccess;

    const peakCurrent = 2 * state.loadCurrent.value / (1 - state.idleFraction);
    const chargeTime = state.inductance.value * peakCurrent / state.inputVoltage.value;
    const dischargeTime = state.inductance.value * peakCurrent / (state.outputVoltage.value - state.inputVoltage.value);
    const cycleTime = (chargeTime + dischargeTime) / (1 - state.idleFraction);
    const result = 1 / cycleTime;
    return <div>
        <VoltageInput stateAccess={stateAccess} k="inputVoltage" />
        <VoltageInput stateAccess={stateAccess} k="outputVoltage" />
        <InductanceInput stateAccess={stateAccess} k="inductance" />
        <CurrentInput stateAccess={stateAccess} k="loadCurrent" />
        <div>
            Charge Time: {formatValue(chargeTime, timeUnits)}
            <RangeIndicator log value={chargeTime} min={1e-6} max={1e-3} units={timeUnits}
                minWarning="Try to use a larger inductor" maxWarning="Try to use a smaller inductor" />
        </div>
        <div>
            Discharge Time: {formatValue(dischargeTime, timeUnits)}
            <RangeIndicator log value={dischargeTime} min={1e-6} max={1e-3} units={timeUnits}
                minWarning="Try to use a larger inductor" maxWarning="Try to use a smaller inductor" />
        </div>
        <div>
            Switching Frequency: {formatValue(result, frequencyUnits)}
            <RangeIndicator log value={result} min={5e3} max={200e3} maxWarning="The frequency is rather high   " units={frequencyUnits} />
        </div>
    </div>
}