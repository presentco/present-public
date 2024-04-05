import React, {Component} from 'react';
import PlacesAutocomplete, { geocodeByAddress, getLatLng } from 'react-places-autocomplete';
import './style.css';
import _ from 'lodash';

class AutoCompleteAddressInput extends Component {

  constructor(props) {
    super(props)
    this.state = {
      address: props.value,
      location:{}
    }
    this.onChange = this.onChange.bind(this);

  }

  onChange(address){
    this.setState({address});
    if (address && this.props.type !== 'discovery'){
      this.props.clickInput(true);
    }
  }

  formatLocationName(address){
    return address.split(',')[0];
  }

  changeAddress(address){
    this.setState({address});

  }

  handleSelect(address){
    let locationName = this.formatLocationName(address);
    geocodeByAddress(address)
       .then(results => {
           getLatLng(results[0]).then(latLng => {
          this.props.getLocation(latLng, locationName);
        });
       }).catch(error => {
           console.error('Error', error)
      })

    this.changeAddress(address);
  }

  render() {

    const inputProps = {
            placeholder: this.props.placeholder,
            value:this.state.address,
            onChange: this.onChange,
            autoFocus: this.props.type === 'discovery'
          },
          cssClasses = {
            autocompleteContainer: 'my-autocomplete-container',
            input: 'edit-circle-input',
            root: 'root',
            autocompleteItem:'autocompleteItem',
            googleLogoImage: 'googleLogoImage',
            autocompleteItemActive: 'autocompleteItemActive'
          };

    let that = this,
        options = {};


    if(this.props.type === 'discovery'){
       options = {
        location: this.props.location,
        radius: 2000,
        types:['(regions)'],
        componentRestrictions: {country: "us"}
      }
    }

    return (
          <PlacesAutocomplete
            inputProps={inputProps}
            onSelect={that.handleSelect.bind(that)}
            placeholder={that.props.placeholder}
            autoFocus={true}
            classNames={cssClasses}
            options={options}/>
    );
  }
}

export default AutoCompleteAddressInput;
