
import React, { Component} from 'react';
// import MaskedInput from 'react-text-mask';
import './style.css';

class EnterVerifyCode extends Component {
  constructor(props){
    super(props);

    this.state={
      code : ''
    }

    this.onChangeValue = this.onChangeValue.bind(this);
  }

  onChangeValue(e){
    let newInput = e.target.value;
      newInput = newInput.replace("(", "");
      newInput = newInput.replace(")", "");
      newInput = newInput.replace("-", "");
      newInput = newInput.replace(" ", "");

      if(newInput.length <= this.props.matchedCode){
          this.props.sendCode(newInput);
          this.setState({code: newInput});
      }

  }

  formatPhone(phone){
    let newPhone = ""
    for (let i = 0; i < phone.length; i++) {
      newPhone += phone[i];
      if(i === 2 || i === 5){
        newPhone += '-';
      }
    }
    return newPhone;
  }


  render() {

    return(
      <div>
        <p className="phone-second-title">Enter Verification Code</p>
        <p className="phone-first-title">Enter the 6 digit code sent to</p>
        <span className="right-phone cursor-poiner" onClick={() => this.props.goback()}>
        +1 {this.formatPhone(this.props.phone)}
        </span>
          <div className="mask-container">
              <input
                className="phone-input-logout"
                value={this.state.code}
                placeholder="Verification Code"
                onChange={this.onChangeValue}/>
          </div>
        {this.props.showError ?
          <p className="error-verify">This code is invalid. Please reenter or resend code</p>
            :
          <span/>}
      </div>
    )
  }
}


export default EnterVerifyCode;
