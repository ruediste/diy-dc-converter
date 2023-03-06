import useLocalStorage from "./useLocalStorage"


const inductanceUnits = {
    F: { label: 'F', factor: 1 },
    mF: { label: 'mF', factor: 1e-3 },
    uF: { label: '\u00b5F', factor: 1e-6 },
};

type InductanceUnit = keyof typeof inductanceUnits;

function SelectInductanceUnit(props: { value: InductanceUnit, onChange: (value: InductanceUnit) => void }) {
    return <select className="form-select" value={props.value} onChange={e => props.onChange(e.target.value as any)} style={{ flexGrow: 0, width: '100px' }}>
        {Object.entries(inductanceUnits).sort(([, a], [, b]) => a.factor - b.factor).map(([key, value]) =>
            <option key={key} value={key}>{value.label}</option>)}
    </select>
}
interface State {
    inputVoltage: number
    outputVoltage: number
    inductance: number
    inductanceUnit: InductanceUnit,
    switchingFrequency: number
    idleFraction: number
}
export function FirstConverter() {
    const [state, setState] = useLocalStorage<State>('firstConverter', {
        inputVoltage: 5,
        outputVoltage: 12,
        inductance: 1e-6,
        inductanceUnit: 'mF',
        switchingFrequency: 0,
        idleFraction: 0.1

    })
    return <div>First converter
        <div className="input-group mb-3">
            <input type="number" className="form-control" value={state.inductance} />
            <SelectInductanceUnit value={state.inductanceUnit} onChange={unit => setState(s => ({ ...s, inductanceUnit: unit }))} />
        </div>
    </div>
}