import React, { ReactNode, useEffect, useState } from "react";
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
function NumberInput<T, K extends KeyOfType<T, number> & string>({ stateAccess: [state, setState], label, k, unit, className }:
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

const resistanceUnits = {
    'Ohm': { label: "\u2126", factor: 1 },
    'kOhm': { label: "k\u2126", factor: 1e3 },
}
const powerUnits = {
    'mW': { label: "mW", factor: 1e-3 },
    'W': { label: "W", factor: 1 },
}

const frequencyUnits = {
    'Hz': { label: "Hz", factor: 1 },
    'kHz': { label: "kHz", factor: 1e3 },
}
type FrequencyUnit = keyof typeof frequencyUnits;
type FrequencyValue = UnitValue<FrequencyUnit>
export function FrequencyInput<T, K extends KeyOfType<T, FrequencyValue> & string>(args: Omit<UnitInputArgs<T, FrequencyUnit, K>, 'units'>) {
    return UnitInput<T, FrequencyUnit, K>({ ...args, units: frequencyUnits });
}


const capacitanceUnits = {
    F: { label: 'F', factor: 1 },
    mF: { label: 'mF', factor: 1e-3 },
    uF: { label: '\u00b5F', factor: 1e-6 },
    nF: { label: 'nF', factor: 1e-9 },
};

const inductanceUnits = {
    H: { label: 'H', factor: 1 },
    mH: { label: 'mH', factor: 1e-3 },
    uH: { label: '\u00b5H', factor: 1e-6 },
    nH: { label: 'nH', factor: 1e-9 },
};
type InductanceUnit = keyof typeof inductanceUnits;
type InductanceValue = UnitValue<InductanceUnit>
function InductanceInput<T, K extends KeyOfType<T, InductanceValue> & string>(args: Omit<UnitInputArgs<T, InductanceUnit, K>, 'units'>) {
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
export function TimeInput<T, K extends KeyOfType<T, TimeValue> & string>(args: Omit<UnitInputArgs<T, TimeUnit, K>, 'units'>) {
    return UnitInput<T, TimeUnit, K>({ ...args, units: timeUnits });
}

const voltageUnits = {
    V: { label: 'V', factor: 1 },
    mV: { label: 'mV', factor: 1e-3 },
};
type VoltageUnit = keyof typeof voltageUnits;
type VoltageValue = UnitValue<VoltageUnit>
function VoltageInput<T, K extends KeyOfType<T, VoltageValue> & string>(args: Omit<UnitInputArgs<T, VoltageUnit, K>, 'units'>) {
    return UnitInput<T, VoltageUnit, K>({ ...args, units: voltageUnits });
}

const currentUnits = {
    A: { label: 'A', factor: 1 },
    mA: { label: 'mA', factor: 1e-3 },
};
type CurrentUnit = keyof typeof currentUnits;
type CurrentValue = UnitValue<CurrentUnit>
function CurrentInput<T, K extends KeyOfType<T, CurrentValue> & string>(args: Omit<UnitInputArgs<T, CurrentUnit, K>, 'units'>) {
    return UnitInput<T, CurrentUnit, K>({ ...args, units: currentUnits });
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
    // var middle: number;
    if (props.log === true) {
        width = 100 * (Math.log(props.value) - Math.log(props.min)) / (Math.log(props.max) - Math.log(props.min));
        // middle = Math.exp(Math.log(props.min) + (Math.log(props.max) - Math.log(props.min)) / 2);
    }
    else {
        width = 100 * (props.value - props.min) / (props.max - props.min);
        // middle = props.min + (props.max - props.min) / 2;
    }
    return <>
        <div style={{ display: 'flex', paddingBottom: '1em', alignItems: 'stretch', flexDirection: 'column', marginBottom: '1rem' }}>
            <div className="progress">
                <div className="progress-bar" role="progressbar" style={{ width: clamp(width, 0, 100) + '%' }}></div>
            </div>
            <div style={{ position: 'relative' }}>
                <span style={{ position: 'absolute', left: 0 }}>{formatValue(props.min, props.units)}</span>
                {/* <span style={{ position: 'absolute', left: '50%' }}>{formatValue(middle, props.units)}</span> */}
                <span style={{ position: 'absolute', right: 0 }}>{formatValue(props.max, props.units)}</span>
            </div>
        </div>
        {width < 0 && props.minWarning !== undefined ? <div className="text-bg-warning p-3">{props.minWarning}</div> : null}
        {width > 100 && props.maxWarning !== undefined ? <div className="text-bg-warning p-3">{props.maxWarning}</div> : null}
    </>
}

class ErrorBoundary extends React.Component<{ children: (hasError: boolean) => ReactNode }, { hasError: boolean }> {
    constructor(props: any) {
        super(props);
        this.state = { hasError: false };
    }

    static getDerivedStateFromError(error: Error) {
        return { hasError: true };
    }

    render() {
        return this.props.children(this.state.hasError);
    }
}

function UseMapLocalStorageHelper<T>(props: { hasError: boolean, initialValue: T, access: [T, Dispatch<SetStateAction<T>>, () => void], map: (access: [T, Dispatch<SetStateAction<T>>, () => void]) => JSX.Element }) {
    useEffect(() => {
        if (props.hasError) {
            props.access[2]();
        }
    });
    return props.map([props.hasError ? props.initialValue : props.access[0], props.access[1], props.access[2]]);
}


export default function useMapLocalStorage<T>(key: string, initialValue: T, map: (access: [T, Dispatch<SetStateAction<T>>, () => void]) => JSX.Element): JSX.Element {
    const access = useLocalStorage(key, initialValue);
    const [resetCount, setResetCount] = useState(0);
    return <ErrorBoundary key={resetCount}>
        {hasError => <UseMapLocalStorageHelper access={[access[0], access[1], () => {
            setResetCount(i => i + 1);
            access[2]();
        }]} hasError={hasError} map={map} initialValue={initialValue} />}
    </ErrorBoundary>
}

interface State {
    inputVoltage: VoltageValue
    outputVoltage: VoltageValue
    inductance: InductanceValue
    idlePercentage: number
    loadCurrent: CurrentValue
    inputRipple: VoltageValue
    outputRipple: VoltageValue
    currentGain: number
}

export function FirstConverter() {
    return useMapLocalStorage<State>('firstConverter', {
        inputVoltage: { value: 5, unit: 'V' },
        outputVoltage: { value: 12, unit: 'V' },
        inputRipple: { value: 50e-3, unit: 'mV' },
        outputRipple: { value: 50e-3, unit: 'mV' },
        inductance: { value: 1e-3, unit: 'mH' },
        idlePercentage: 10,
        loadCurrent: { value: 0.01, unit: 'mA' },
        currentGain: 50
    }, (stateAccess) => {
        const [state] = stateAccess;
        const idleFraction = state.idlePercentage / 100;

        const k = (state.outputVoltage.value - state.inputVoltage.value) / state.inputVoltage.value;
        const peakCurrent = 2 * (k + 1) * state.loadCurrent.value / (1 - idleFraction);
        const chargeTime = state.inductance.value * peakCurrent / state.inputVoltage.value;
        const dischargeTime = state.inductance.value * peakCurrent / (state.outputVoltage.value - state.inputVoltage.value);
        const cycleTime = (chargeTime + dischargeTime) / (1 - idleFraction);
        const switchingFrequency = 1 / cycleTime;
        const loadResistor = state.outputVoltage.value / state.loadCurrent.value;
        const outputPower = state.outputVoltage.value * state.loadCurrent.value;
        const outputCapacitance = peakCurrent * dischargeTime / (2 * state.outputRipple.value);
        const inputCapacitance = peakCurrent * chargeTime / (2 * state.inputRipple.value);
        const baseCurrent = peakCurrent / state.currentGain;
        const baseResistor = (3.3 - 0.6) / baseCurrent;

        return <div className="toolContainer">
            <div className="row">
                <VoltageInput stateAccess={stateAccess} k="inputVoltage" className="col" />
                <VoltageInput stateAccess={stateAccess} k="outputVoltage" className="col" />
            </div>
            <div className="row">
                <InductanceInput stateAccess={stateAccess} k="inductance" className="col" />
                <CurrentInput stateAccess={stateAccess} k="loadCurrent" className="col" />
            </div>
            <div className="row">
                <NumberInput stateAccess={stateAccess} k="idlePercentage" unit="%" className="col" />
                <NumberInput stateAccess={stateAccess} k="currentGain" className="col" label="Transistor Current Gain" />
            </div>
            <div className="row">
                <VoltageInput stateAccess={stateAccess} k="inputRipple" className="col" />
                <VoltageInput stateAccess={stateAccess} k="outputRipple" className="col" />
            </div>
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
                Switching Frequency: {formatValue(switchingFrequency, frequencyUnits)}
                <RangeIndicator log value={switchingFrequency} min={5e3} max={200e3} maxWarning="The frequency is rather high" units={frequencyUnits} />
            </div>
            <div> Peak Inductor Current: {formatValue(peakCurrent, currentUnits)}
                <RangeIndicator value={peakCurrent} min={1e-3} max={1} maxWarning="Make sure your inductor and your transistor can handle this current" units={currentUnits} />
            </div>
            <div> Load Resistor: {formatValue(loadResistor, resistanceUnits)} </div>
            <div> Output Power: {formatValue(outputPower, powerUnits)}
                <RangeIndicator value={outputPower} min={1e-3} max={250e-3} maxWarning="Typical small resistors are rated for 1/8W or 1/4W. Make sure to use an adequate resistor" units={powerUnits} />
            </div>
            <div className="row">
                <div className="col">
                    <div> Duty: {round(chargeTime / cycleTime * 100, 2)}% </div>
                    <div> Base Resistor: {formatValue(baseResistor, resistanceUnits)} </div>
                </div>
                <div className="col">
                    <div> Output Capacitor: {formatValue(outputCapacitance, capacitanceUnits)} </div>
                    <div> Input Capacitor: {formatValue(inputCapacitance, capacitanceUnits)} </div>
                </div>
            </div>
            <div>
                <button type="button" className="btn btn-secondary" onClick={stateAccess[2]}>Reset</button>
            </div >
        </div >
    });
}