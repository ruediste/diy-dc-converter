import { CartesianGrid, DefaultLegendContent, Label, Legend, Line, LineChart, Tooltip, XAxis, YAxis } from "recharts";
import { chooseUnitInfo, CurrentInput, currentUnits, CurrentValue, formatValue, frequencyUnits, InductanceInput, InductanceValue, NumberInput, powerUnits, timeUnits, UnitInfos, VoltageInput, voltageUnits, VoltageValue } from "./Input";
import { useMapLocalStorage } from "./useLocalStorage"
import { ReactNode, useState } from "react";


interface State {
    inductance: InductanceValue
    peakCurrent: CurrentValue
    idlePercentage: number

    in: VoltageValue
    inMin: VoltageValue
    inMax: VoltageValue
    inSteps: number

    out: VoltageValue
    outMin: VoltageValue
    outMax: VoltageValue
}

const colors = ['#1b9e77', '#d95f02', '#7570b3', '#e7298a', '#66a61e', '#e6ab02', '#a6761d', '#666666'];

type GraphProps = {
    xAxisLabel: string,
    xUnits: UnitInfos,
    xValues: number[],

    yAxisLabel: string,
    yUnits: UnitInfos,
} & ({
    f: (x: number) => number | undefined
} | {
    f: (x: number, s: number) => number | undefined
    series: number[],
    seriesUnits: UnitInfos,
    legendTitle?: string
})


function Graph(p: GraphProps) {
    const data: ({ x: number } & { [key: number]: number })[] = [];
    let maxX = 0;
    let maxY = 0;
    p.xValues.forEach(x => {
        maxX = Math.max(maxX, x);
        const point: { x: number } & { [key: number]: number } = { x };
        if ('series' in p) {
            p.series.forEach((s, idx) => {
                const value = p.f(x, s);
                if (value !== undefined) {
                    point[idx] = value;
                    maxY = Math.max(maxY, value);
                }
            });
        }
        else {
            const value = p.f(x);
            if (value !== undefined) {
                point[0] = value;
                maxY = Math.max(maxY, value);
            }
        }
        data.push(point);
    });
    const xUnit = chooseUnitInfo(maxX, p.xUnits);
    const yUnit = chooseUnitInfo(maxY, p.yUnits);
    return <LineChart width={730} height={500} data={data}
        margin={{ top: 5, right: 30, left: 40, bottom: 20 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="x" tickFormatter={(x: any) => formatValue(x, xUnit, false)} >
            <Label dy={20} value={`${p.xAxisLabel} [${xUnit.label}]`} />
        </XAxis>
        <YAxis tickFormatter={(x: any) => formatValue(x, yUnit, false)}  >
            <Label dx={-50} angle={-90} value={`${p.yAxisLabel} [${yUnit.label}]`} />
        </YAxis>
        {'series' in p ?
        <Legend verticalAlign="top" align="right" layout="vertical" content={(legendProps)=>{
            const {ref, ...others}=legendProps;
            return <>
                {p.legendTitle!==null?<span>{p.legendTitle}</span>:null}
                <DefaultLegendContent {...others} />
            </>;
        }}/>:null
    }
        <Tooltip labelFormatter={x => formatValue(x, xUnit)} formatter={(x: any) => formatValue(x, p.yUnits)} />
        {'series' in p ? p.series.map((s, idx) =>
            <Line key={idx} type="monotone" dataKey={idx} dot={false} name={formatValue(s, p.seriesUnits)}
                stroke={colors[idx % colors.length]} />)
            :
            <Line type="monotone" dataKey={0} dot={false}
                stroke={colors[0]} />
        }
    </LineChart>
}

function GraphSwitcher(p: {graphs: {label: string, render: () => ReactNode}[]}){
    const [graphIdx, setGraphIdx]=useState(0);
    return <>
            <div className="btn-group" role="group" aria-label="Basic example">
                {p.graphs.map((g, idx) =>
                    <button key={idx} type="button" className={`btn btn-outline-secondary${idx === graphIdx ? ' active' : ''}`}
                        onClick={() => setGraphIdx(idx)}
                    >{g.label}</button>)}
            </div>
            {p.graphs[graphIdx].render()}
        </>
}

function chargeTime(state: State, inputVoltage: number) {
    return state.inductance.value * state.peakCurrent.value / inputVoltage;
}

function dischargeTime(state: State, inputVoltage: number, outputVoltage: number){
    return state.inductance.value * state.peakCurrent.value  / (outputVoltage - inputVoltage);
}

function calc(state: State, inputVoltage: number, outputVoltage: number) {
    const idleFraction = state.idlePercentage / 100;
    const peakCurrent = state.peakCurrent.value;
    const inductance = state.inductance.value;

    const dischargeTimeValue = dischargeTime(state, inputVoltage, outputVoltage);
    const minCycleTime = (chargeTime(state, inputVoltage) + dischargeTimeValue) / (1-idleFraction);
    const maxSwitchingFrequency = 1 / minCycleTime;
    const maxCurrent = peakCurrent * dischargeTimeValue / (2 * minCycleTime);
    return { dischargeTime: dischargeTimeValue, maxSwitchingFrequency, maxCurrent, maxPower: maxCurrent * outputVoltage };
}

function calcForFrequency(state: State, frequency: number){
    const current=state.peakCurrent.value*dischargeTime(state,state.in.value, state.out.value)/(2*1/frequency);
    return {current};
}

function range(start: number, stop: number, count: number) {
    const result: number[] = [];
    if (count <= 1) {
        result.push((start + stop) / 2);
    }
    else {
        for (let i = 0; i < count; i++) {
            result.push(start + i * (stop - start) / (count - 1));
        }
    }
    return result;
}

export function Cot() {
    return useMapLocalStorage<State>('cot', {
        inductance: { value: 1e-3, unit: 'mH' },
        peakCurrent: { value: 0.01, unit: 'mA' },
        idlePercentage: 10,
        in: { value: 5, unit: 'V' },
        inMin: { value: 5, unit: 'V' },
        inMax: { value: 12, unit: 'V' },
        inSteps: 5,
        out: { value: 12, unit: 'V' },
        outMin: { value: 5, unit: 'V' },
        outMax: { value: 20, unit: 'V' },
    }, (stateAccess) => {
        const [state, setState] = stateAccess;
        const inVoltages = range(state.inMin.value, state.inMax.value, state.inSteps);
        const outputVoltages = range(state.outMin.value, state.outMax.value, 200);
        const xAxisOutputVoltageProps = {
            xValues: outputVoltages,
            xAxisLabel: "Output Voltage",
            xUnits: voltageUnits
        }
        const defaultGraphProps = {
            ...xAxisOutputVoltageProps,
            series: inVoltages,
            legendTitle: 'Input Voltages',
            seriesUnits: voltageUnits
        }
        const filterVoltage = (f: (outputVoltage: number, inputVoltage: number) => number) =>
            (outputVoltage: number, inputVoltage: number) => outputVoltage < inputVoltage * 1.1 ? undefined : f(outputVoltage, inputVoltage);

        const graphs: { label: string, render: () => ReactNode }[] = [
            {
                label: "Max Output Current", render: () => <Graph
                    {...defaultGraphProps}
                    f={filterVoltage((outputVoltage, inputVoltage) => calc(state, inputVoltage, outputVoltage).maxCurrent)}
                    yAxisLabel="Max Output Current" yUnits={currentUnits}
                />
            },
            {
                label: "Max Output Power", render: () => <Graph
                    {...defaultGraphProps}
                    f={filterVoltage((outputVoltage, inputVoltage) => calc(state, inputVoltage, outputVoltage).maxPower)}
                    yAxisLabel="Max Output Power" yUnits={powerUnits}
                />
            },
            {
                label: "Max Switching Frequency", render: () => <Graph
                    {...defaultGraphProps}
                    f={filterVoltage((outputVoltage, inputVoltage) => calc(state, inputVoltage, outputVoltage).maxSwitchingFrequency)}
                    yAxisLabel="Max Switching Frequency" yUnits={frequencyUnits}
                />
            },
            {
                label: "Discharge Time", render: () => <Graph
                    {...defaultGraphProps}
                    f={filterVoltage((outputVoltage, inputVoltage) => calc(state, inputVoltage, outputVoltage).dischargeTime)}
                    yAxisLabel="Discharge Time" yUnits={timeUnits}
                />
            },
            {
                label: "Charge Time", render: () => <Graph
                    xAxisLabel="Input Voltage"
                    xUnits={voltageUnits}
                    xValues={range(state.inMin.value, state.inMax.value, 200)}
                    yAxisLabel="Charge Time"
                    yUnits={timeUnits}
                    f={(inputVoltage: number) => chargeTime(state, inputVoltage)}
                />
            },
        ]

        const singlePointResult=calc(state, state.in.value, state.out.value);
        return <div className="toolContainer">
            <div className="row">
                <InductanceInput stateAccess={stateAccess} k="inductance" className="col" />
                <CurrentInput stateAccess={stateAccess} k="peakCurrent" className="col" />
                <NumberInput stateAccess={stateAccess} k="idlePercentage" unit="%" className="col" />
            </div>
            <div className="row">
                <VoltageInput stateAccess={stateAccess} k="inMin" className="col" />
                <VoltageInput stateAccess={stateAccess} k="inMax" className="col" />
                <NumberInput stateAccess={stateAccess} k="inSteps" className="col" />
            </div>
            <div className="row">
                <VoltageInput stateAccess={stateAccess} k="outMin" className="col" />
                <VoltageInput stateAccess={stateAccess} k="outMax" className="col" />
            </div>
            <GraphSwitcher graphs={graphs}/>
{/*             
            <h1> Single Operation Point</h1>
            <div className="row">
                <VoltageInput stateAccess={stateAccess} k="in" className="col" />
                <VoltageInput stateAccess={stateAccess} k="out" className="col" />
            </div>
            <Graph
            xAxisLabel="Frequency"
            xUnits={frequencyUnits}
            xValues={range(1, singlePointResult.maxSwitchingFrequency, 200)}
            yAxisLabel="Output Current"
            yUnits={currentUnits}
            f={(frequency: number)=>calcForFrequency(state, frequency).current}
            /> */}
            <div>
                <button type="button" className="btn btn-secondary" onClick={stateAccess[2]}>Reset</button>
            </div >
        </div >
        // <Plot.OfX y={(x) => Math.sin(x)} />
    });
}