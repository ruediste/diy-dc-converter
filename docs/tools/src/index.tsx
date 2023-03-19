import React, { useState } from 'react';
import ReactDOM from 'react-dom/client';
import './index.scss';
import App from './App';
import { FirstConverter } from './FirstConverter';

const tools: { [key: string]: () => JSX.Element } = {
  app: () => <App />,
  first: () => <FirstConverter />
};

function DevSelector() {
  const [selectedTool, setSelectedTool] = useState<[string, () => JSX.Element]>(['first', tools.first])
  return <div style={{ display: 'flex', justifyContent: 'center' }}><div style={{ width: '680px' }}><select className="form-select" value={selectedTool[0]} onChange={e => setSelectedTool([e.target.value, tools[e.target.value]])}>
    {Object.entries(tools).sort(([a], [b]) => a.localeCompare(b)).map(([e]) =>
      <option key={e} value={e}>{e}</option>)}
  </select>
    {selectedTool[1]()}
  </div>
  </div>
}

if (document.querySelector('meta[name="generator"]') != null) {
  document.querySelectorAll('div[data-tool]').forEach(element => {
    const tool = element.getAttribute('data-tool')!;
    const root = ReactDOM.createRoot(element);
    root.render(
      <React.StrictMode>
        {tools[tool]()}
      </React.StrictMode>
    );
  })
}
else {
  while (document.body.lastChild) {
    document.body.removeChild(document.body.lastChild);
  }
  const element = document.createElement('div');
  document.body.appendChild(element);
  const root = ReactDOM.createRoot(element);
  root.render(
    <React.StrictMode>
      <DevSelector />
    </React.StrictMode>
  );
}