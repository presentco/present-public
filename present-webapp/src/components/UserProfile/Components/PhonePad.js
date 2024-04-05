import React, { Component} from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as userActions from '../../../redux/actions/userActions';
import ApiHandler from '../../../core/ApiHandler';
import MaskedInput from 'react-input-mask';

class PhonePad extends Component {

  constructor(props){
    super(props);

    this.state={
      status:'phone',
      phone: '',
      code: ''
    }

    this.onChangeValue = this.onChangeValue.bind(this);
    this.onClickClear = this.onClickClear.bind(this);
    this.onEnterPhone = this.onEnterPhone.bind(this);
    this.onEnterResend = this.onEnterResend.bind(this);
  }

  onChangeValue(e){
    let newInput = e.target.value;
      newInput = newInput.replace("(", "");
      newInput = newInput.replace(")", "");
      newInput = newInput.replace("-", "");
      newInput = newInput.replace(" ", "");

    if(this.state.status === 'phone'){
      this.setState({phone: newInput});
    } else{
      if(e.target.value.length <= this.state.matchedCode){
        this.setState({code: newInput});
      }
    }
  }

  onEnterPhone(){
    if(this.state.status === 'phone'){
      this.setState({status: 'code'});
      this.requestVerification();
    } else {
      this.verifyCode();
    }
  }

  requestVerification(val){
    let request = ApiHandler.attachHeader({phoneNumber: `1${this.state.phone}`});
    this.props.actions.verifyPhoneNumber(request).then(response => {
      this.setState({status: 'code', matchedCode:response.codeLength});
    }).catch(error => {
      throw(error);
    });
  }

  verifyCode(){
    let request=ApiHandler.attachHeader({code: this.state.code});
    this.props.actions.verifyCode(request).then(response => {
      if(response.userProfile){
        this.setState({status:'confirmed'});
        this.props.loadRequestedUserFromUrl();
      }

      if(response.data.error.message === 'Invalid verification code.'){
        this.setState({showError:true})
      }
    }).catch(error => {
      if(error){
        this.setState({showError:true})
      }
    });
  }


  onEnterResend(){
    this.requestVerification();
  }

  onClickClear(){
    if(this.state.status === 'phone'){
      let phone = this.state.phone,
          newPhone = phone.slice(0, -1);

          if(phone.length > 0){
            this.setState({phone: newPhone});
          }
    } else{
      let code = this.state.code,
          newCode = code.slice(0, -1);

          if(code.length > 0){
            this.setState({code: newCode});
          }
    }
  }

  onClickNumberTapped(val){
    if(this.state.status === 'phone'){
      let phone = this.state.phone,
          newPhone = `${phone}${val}`;

      this.setState({phone: newPhone});
    } else {
      let code = this.state.code,
          newCode = `${code}${val}`;
        if(code.length < this.state.matchedCode){
          this.setState({code: newCode});
        }
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

  render(){
    if(this.state.status === 'confirmed'){
      return(
        <div className="row">
          <div className="small-11 small-centered medium-11 medium-centered large-11 large-centered main-phone-container text-center columns">
            <p className="verify-text-confirm">Your Phone has been verified</p>
          </div>
        </div>
      )
    } else {
      return(
        <div className="row">
          <div className="small-11 small-centered medium-11 medium-centered large-11 large-centered main-phone-container text-center columns">
           <p className="welcome-title">{this.state.status === 'phone' ? 'Enter your phone number' : 'Enter your verification code'}</p>
           {this.state.status === 'phone' ?
             <p className="welcome-second-title">To sign up for Present</p> :
               <p>Sent to: <span className="right-phone">{this.formatPhone(this.state.phone)}</span></p>
            }
           {
             this.state.status === 'phone' ?
             <MaskedInput
               mask='(999) 999-9999'
               value={this.state.phone}
               className="phone-input text-center"
               onChange={this.onChangeValue}
               placeholder="(111)123-4567"/>
               :
             <input
               className="phone-input text-center"
               value={this.state.code}
               placeholder="111 111"
               onChange={this.onChangeValue}/>
           }

           <div className="row phone-container">
             <div className="small-4 medium-4 large-4 phone-number columns" onClick={this.onClickNumberTapped.bind(this,'1')} >1</div>
             <div className="small-4 medium-4 large-4 phone-number columns" onClick={this.onClickNumberTapped.bind(this,'2')} >2</div>
             <div className="small-4 medium-4 large-4 phone-number columns" onClick={this.onClickNumberTapped.bind(this,'3')} >3</div>
             <div className="small-4 medium-4 large-4 phone-number columns" onClick={this.onClickNumberTapped.bind(this,'4')} >4</div>
             <div className="small-4 medium-4 large-4 phone-number columns" onClick={this.onClickNumberTapped.bind(this,'5')} >5</div>
             <div className="small-4 medium-4 large-4 phone-number columns" onClick={this.onClickNumberTapped.bind(this,'6')} >6</div>
             <div className="small-4 medium-4 large-4 phone-number columns" onClick={this.onClickNumberTapped.bind(this,'7')} >7</div>
             <div className="small-4 medium-4 large-4 phone-number columns" onClick={this.onClickNumberTapped.bind(this,'8')} >8</div>
             <div className="small-4 medium-4 large-4 phone-number columns" onClick={this.onClickNumberTapped.bind(this,'9')} >9</div>
             <div className="small-4 medium-4 large-4 phone-number-transparent columns">1</div>
             <div className="small-4 medium-4 large-4 phone-number columns" onClick={this.onClickNumberTapped.bind(this,'0')} >0</div>
             <div className="small-4 medium-4 large-4 column " onClick={this.onClickClear}>
               <img src={require('../../../assets/images/clear-btn@2x.png')} className="clear-btn" alt="clear"/>
             </div>
           </div>
           {this.state.showError ?
           <p className="error-verify">This code is invalid. Please reenter or resend code</p> : <span/>}
           <button className="take-tour-btn next-btn" onClick={this.onEnterPhone}>Next</button>
           {this.state.status !== 'phone' ?
             <p className="resent-text cursor-poiner" onClick={this.onEnterResend}>Resend Code</p>
             : <span/>}
        </div>
        </div>
      )
  }
}
}

function mapStateToProps(state, ownProps) {
  return {};
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(userActions, dispatch)
  };
}

export default connect(mapStateToProps,mapDispatchToProps)(PhonePad);
