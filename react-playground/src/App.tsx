import React, { useState } from "react";
import ListGroup from "./components/ListGroup";
import Alert from "./components/Alert";
import Button from "./components/Button";

function App() {
  let items = ["New York", "San Francisco", "Tokyo", "London", "Paris"];
  const handleSelectItem = (item: string) => {
    console.log(item);
  };
  const [alertInvisible, setAlertInvisible] = useState(true);
  const handleClick = () => {
    console.log("Button clicked");
    setAlertInvisible(false);
  };
  return (
    <div>
      <ListGroup items={items} heading="Cities" onSelectItem={handleSelectItem} />
      <hr></hr>
      <div>
        {!alertInvisible && (
          <Alert onClose={() => setAlertInvisible(true)}>
            Hello <span>World</span>
          </Alert>
        )}
      </div>
      <Button color="danger" onClick={handleClick}>Click</Button>
    </div>
  );
}

export default App;
