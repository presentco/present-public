
import React, { Component} from 'react';
import MaskedInput from 'react-input-mask';
import './style.css';

class EnterPhoneNumber extends Component {
  constructor(props){
    super(props);

    this.state={
      phone: ''
    }

    this.onChangeValue = this.onChangeValue.bind(this);
  }

  onChangeValue(e){

    let newInput = e.target.value;
      newInput = newInput.replace("(", "");
      newInput = newInput.replace(")", "");
      newInput = newInput.replace("-", "");
      newInput = newInput.replace(" ", "");

      this.props.sendPhone(newInput);
      this.setState({phone: newInput});
  }


  render() {

    return(
      <div>
        <p className="phone-second-title">Verify phone number</p>
        <p className="phone-first-title">Enter your phone number to sign up for Present</p>
          <div className="mask-container">
            <span className="phone-pre">+ 1</span>
            <MaskedInput
              {...this.props}
              mask="(999) 999-9999"
              maskChar={null}
              value={this.state.phone}
              className="phone-input-logout"
              onChange={this.onChangeValue}
            />
          </div>
      </div>
    )
  }
}


export default EnterPhoneNumber;
