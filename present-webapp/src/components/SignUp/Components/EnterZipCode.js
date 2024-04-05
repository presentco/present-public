
import React, { Component} from 'react';
import './style.css';

class EnterZipCode extends Component {
  constructor(props){
    super(props);

    this.state={
        zipCode:''
    }

    this.onChangeInput = this.onChangeInput.bind(this);
  }

  onChangeInput(e){
    e.preventDefault();
    if(e.target.value.length <= 5){
      this.setState({zipCode: e.target.value});
    }
  }

  render() {

    return(
      <div className={this.props.type ? "mobile-view" : "main-form-container"}>
          <div className="form-container">
            <p className="step-title">What&apos;s your zip code?</p>
            <p className="sec-title zip-first">Present connects you to extraordinary people nearby.</p>
            <p className="sec-title zip-second">We will never share your location.</p>
            <div className="photo-upload-container">

            <input
              name="zipCode"
              value={this.state.zipCode}
              type="number"
              onChange={this.onChangeInput}
              className="input-form" />

              <label className="field-label">Zip Code</label>
              <button disabled={this.state.zipCode.length === 5 ? false : true } onClick={() => this.props.onClickThatsMe(this.state.zipCode)} className="thatsme-btn">
                That&apos;s It!
              </button>
            </div>
          </div>
      </div>
    )
  }
}


export default EnterZipCode;
