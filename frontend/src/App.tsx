import "./App.scss";
import {
  LineChart,
  Line,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  Label,
  ResponsiveContainer,
  Brush,
} from "recharts";
import { useEffect, useRef, useState } from "react";
import { useWebSocket } from "react-use-websocket/dist/lib/use-websocket";
import { getSiPrefix, SiPrefix, siPrefixes } from "./siPrefix";
import { time } from "console";
const data = [{ name: "Page A", uv: 400, pv: 2400, amt: 2400 }];

interface Series {
  name: string;
  unit: string;
  unitSymbol: string;
  stepAfter: boolean;
  yAxisIndex: number;
}
interface PlotValues {
  time: number;
  values: number[];
}

interface Plot {
  title: string;
  start: number;
  end: number;
  samplePeriod: number | null;
  timePrefix: SiPrefix;

  series: Series[];
  values: PlotValues[];

  axes: { index: number; unit: string; unitSymbol: string; isRight: boolean }[];
}

const palette = [
  "#68affc",
  "#4c319e",
  "#10eddc",
  "#115d52",
  "#90ea66",
  "#31a46c",
  "#aedfca",
  "#2a538a",
];

function paletteColor(idx: number) {
  return palette[idx % palette.length];
}

function DisplayPlot({
  plot,
  height,
}: {
  plot: Plot;
  height?: string | number;
}) {
  const timeFormatter = (x: number) =>
    (x / plot.timePrefix.multiplier).toFixed(3);
  return (
    <ResponsiveContainer height={height}>
      <LineChart
        data={plot.values}

        // syncId="anyId"
      >
        {plot.series.map((series, idx) => (
          <Line
            key={series.name}
            type={series.stepAfter ? "stepAfter" : "linear"}
            dataKey={(x) => x.values[idx]}
            name={series.name + "[" + series.unitSymbol + "]"}
            stroke={paletteColor(idx)}
            fill={paletteColor(idx)}
            yAxisId={series.yAxisIndex}
            strokeWidth={3}
            animationDuration={0}
          />
        ))}
        <CartesianGrid stroke="#ccc" />
        <XAxis
          dataKey="time"
          type="number"
          tickFormatter={timeFormatter}
          unit={plot.timePrefix.symbol + "s"}
        />
        {plot.axes.map((axis) => (
          <YAxis
            key={axis.index}
            yAxisId={axis.index}
            label={<Label value={axis.unitSymbol} position="insideBottom" />}
            orientation={axis.isRight ? "right" : "left"}
          />
        ))}
        <Tooltip
          labelFormatter={(x) =>
            timeFormatter(x) + plot.timePrefix.symbol + "s"
          }
          formatter={(value: any, name, item, index) => {
            const siPrefix = getSiPrefix(value);
            const series = plot.series[index];
            return (
              (value / siPrefix.multiplier).toFixed(3) +
              siPrefix?.symbol +
              series.unitSymbol
            );
          }}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}
function App() {
  const [plots, setPlots] = useState<Plot[]>();
  const load = useRef<() => void>();
  useEffect(() => {
    const doLoad = () =>
      fetch("/api/plot")
        .then((response) => response.json())
        .then((p) => setPlots(p));
    load.current = doLoad;
    doLoad();
  }, []);
  const { sendMessage, lastMessage, readyState } = useWebSocket(
    "ws://localhost:35729/livereload",
    {
      onOpen: () => {
        console.log("WebSocket connection established.");
        sendMessage(
          JSON.stringify({
            command: "hello",
            protocols: ["http://livereload.com/protocols/official-7"],
          })
        );
      },
      onMessage: (event) => {
        const msg = JSON.parse(event.data);
        if (msg.command === "reload") {
          load.current!();
        }
      },
    }
  );

  const [zoomedIdx, setZoomedIdx] = useState<number>();

  if (plots === undefined) return <>Loading...</>;
  return (
    <>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)" }}>
        {plots.map((plot, idx) => (
          <div onClick={() => setZoomedIdx(idx)}>
            <h2>{plot.title}</h2>
            <DisplayPlot key={idx} plot={plot} height={400} />
          </div>
        ))}
      </div>
      {zoomedIdx === undefined ? null : (
        <div className="fullScreen" onClick={() => setZoomedIdx(undefined)}>
          <h2>{plots[zoomedIdx].title}</h2>
          <div>
            <DisplayPlot plot={plots[zoomedIdx]} />
          </div>
        </div>
      )}
    </>
  );
}

export default App;
